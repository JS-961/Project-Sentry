package com.safedrive.ai.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.safedrive.ai.MainActivity
import com.safedrive.ai.R
import com.safedrive.ai.SafeDriveApplication
import com.safedrive.ai.data.DrivingStateStore
import com.safedrive.ai.data.RiskCounters
import com.safedrive.ai.data.local.CrashAlertEntity
import com.safedrive.ai.data.local.RiskEventEntity
import com.safedrive.ai.data.local.TripEntity
import com.safedrive.ai.ui.CrashCountdownActivity
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class DrivingModeService : Service(), SensorEventListener {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val app by lazy { application as SafeDriveApplication }
    private val tripDao by lazy { app.database.tripDao() }
    private val riskEventDao by lazy { app.database.riskEventDao() }
    private val crashAlertDao by lazy { app.database.crashAlertDao() }
    private val settingsRepository by lazy { app.settingsRepository }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private val fusedLocation by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private var isDriving = false
    private var currentTripId: Long? = null
    private var latestLocation: Location? = null
    private var latestSpeedMps = 0f
    private var lastSpeedMps = 0f
    private var lastSpeedUpdateMs = 0L
    private var lastAccelMagnitude = SensorManager.GRAVITY_EARTH
    private var lastAccelUpdateMs = 0L
    private var currentGyroZ = 0f
    private var lastNotificationRisk = Int.MIN_VALUE
    private var lastNotificationSpeedKmh = Int.MIN_VALUE
    private var lastNotificationUpdateMs = 0L
    private var lastSensorHeartbeatPublishMs = 0L

    private var lastHarshBrakeMs = 0L
    private var lastHarshAccelMs = 0L
    private var lastCorneringMs = 0L
    private var lastSpeedingMs = 0L
    private var lastCrashMs = 0L

    private var riskScore = 0
    private var counters = RiskCounters()
    private var maxRiskScore = 0
    private var riskScoreSum = 0L
    private var riskScoreSamples = 0L

    private var decayJob: Job? = null
    private var crashValidationJob: Job? = null
    private var pendingCrash: PendingCrash? = null
    private var pendingCrashCandidate: CrashCandidate? = null

    private val sensorBuffer = CircularSensorBuffer(capacity = 2048)
    private val speedHistory = ArrayDeque<SpeedPoint>()

    private val locationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMinUpdateIntervalMillis(500L)
            .build()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (location in result.locations) {
                processLocation(location)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        when (action) {
            ACTION_START_DRIVING -> startDrivingMode()
            ACTION_STOP_DRIVING -> stopDrivingMode()
            ACTION_SIMULATE_CRASH -> {
                if (!isDriving) startDrivingMode()
                if (!isDriving) return START_NOT_STICKY
                triggerCrashCountdown(
                    simulated = true,
                    reason = "Simulated crash",
                )
            }
            ACTION_CRASH_CANCELLED -> handleCrashCancelled()
            ACTION_CRASH_TIMEOUT -> handleCrashEscalation(manualCall = false)
            ACTION_CRASH_CALL_NOW -> handleCrashEscalation(manualCall = true)
            else -> Log.w(TAG, "Ignoring unknown service action: $action")
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopSensorsAndLocation()
        decayJob?.cancel()
        crashValidationJob?.cancel()
        tts?.shutdown()
        tts = null
        ttsReady = false
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startDrivingMode() {
        if (isDriving) return
        if (!hasLocationPermission()) {
            DrivingStateStore.setLatestEvent("Location permission is required to start monitoring")
            stopSelf()
            return
        }
        isDriving = true
        counters = RiskCounters()
        riskScore = 0
        maxRiskScore = 0
        riskScoreSum = 0
        riskScoreSamples = 0
        lastSpeedMps = 0f
        lastSpeedUpdateMs = 0L
        lastAccelMagnitude = SensorManager.GRAVITY_EARTH
        lastAccelUpdateMs = 0L
        currentGyroZ = 0f
        latestLocation = null
        latestSpeedMps = 0f
        lastNotificationRisk = Int.MIN_VALUE
        lastNotificationSpeedKmh = Int.MIN_VALUE
        lastNotificationUpdateMs = 0L
        lastSensorHeartbeatPublishMs = 0L
        lastHarshBrakeMs = 0L
        lastHarshAccelMs = 0L
        lastCorneringMs = 0L
        lastSpeedingMs = 0L
        lastCrashMs = 0L
        pendingCrash = null
        pendingCrashCandidate = null
        crashValidationJob?.cancel()
        speedHistory.clear()
        sensorBuffer.clear()

        startForeground(NOTIFICATION_ID, buildDrivingNotification())
        captureNotificationSnapshot()
        startSensorsAndLocation()
        DrivingStateStore.setDrivingActive(true)
        DrivingStateStore.updateRisk(riskScore = 0, counters = counters)
        DrivingStateStore.setLatestEvent("Monitoring started")
        startRiskDecayLoop()

        serviceScope.launch {
            currentTripId = tripDao.insert(
                TripEntity(
                    startedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun stopDrivingMode() {
        if (!isDriving) {
            stopSelf()
            return
        }
        isDriving = false
        DrivingStateStore.setCrashCountdownActive(false)
        pendingCrash = null
        pendingCrashCandidate = null
        stopSensorsAndLocation()
        decayJob?.cancel()
        crashValidationJob?.cancel()
        cancelCrashAlertNotification()

        val endedAt = System.currentTimeMillis()
        val avgRisk = if (riskScoreSamples > 0) {
            riskScoreSum.toFloat() / riskScoreSamples.toFloat()
        } else {
            0f
        }
        val totalEvents = counters.total
        val tripId = currentTripId
        currentTripId = null

        if (tripId != null) {
            serviceScope.launch {
                tripDao.endTrip(
                    tripId = tripId,
                    endedAt = endedAt,
                    avgRiskScore = avgRisk,
                    maxRiskScore = maxRiskScore,
                    totalEvents = totalEvents,
                )
            }
        }

        DrivingStateStore.setDrivingActive(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun startSensorsAndLocation() {
        accelerometer?.let { sensorManager.registerListener(this, it, SENSOR_DELAY_US) }
        gyroscope?.let { sensorManager.registerListener(this, it, SENSOR_DELAY_US) }

        if (hasLocationPermission()) {
            fusedLocation.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper(),
            )
        }
    }

    private fun stopSensorsAndLocation() {
        sensorManager.unregisterListener(this)
        fusedLocation.removeLocationUpdates(locationCallback)
    }

    private fun startRiskDecayLoop() {
        decayJob?.cancel()
        decayJob = serviceScope.launch {
            while (isActive && isDriving) {
                delay(1_000L)
                riskScore = max(0, riskScore - RISK_DECAY_PER_SECOND)
                maxRiskScore = max(maxRiskScore, riskScore)
                riskScoreSum += riskScore
                riskScoreSamples += 1
                publishRiskState()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isDriving) return
        val now = System.currentTimeMillis()
        if (now - lastSensorHeartbeatPublishMs >= SENSOR_HEARTBEAT_PUBLISH_MS) {
            lastSensorHeartbeatPublishMs = now
            DrivingStateStore.setSensorHeartbeat(now)
        }
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                val magnitude = sqrt(ax * ax + ay * ay + az * az)
                val linearAccel = abs(magnitude - SensorManager.GRAVITY_EARTH)
                val dtSeconds = ((now - lastAccelUpdateMs).coerceAtLeast(1L)) / 1000f
                val jerk = abs(magnitude - lastAccelMagnitude) / dtSeconds
                lastAccelMagnitude = magnitude
                lastAccelUpdateMs = now

                sensorBuffer.add(
                    SensorSnapshot(
                        timestampMs = now,
                        ax = ax,
                        ay = ay,
                        az = az,
                        linearAccel = linearAccel,
                        gyroZ = currentGyroZ,
                        speedMps = latestSpeedMps,
                    ),
                )

                if (latestSpeedMps >= SHARP_CORNERING_MIN_SPEED_MPS &&
                    linearAccel >= SHARP_CORNERING_MIN_LINEAR_ACCEL &&
                    abs(currentGyroZ) >= SHARP_CORNERING_THRESHOLD_RAD &&
                    now - lastCorneringMs >= EVENT_COOLDOWN_MS
                ) {
                    lastCorneringMs = now
                    registerRiskEvent("SHARP_CORNERING", abs(currentGyroZ))
                }

                val crashImpact = linearAccel >= CRASH_LINEAR_ACCEL_THRESHOLD
                val crashJerk = jerk >= CRASH_JERK_THRESHOLD
                if (crashImpact || crashJerk) {
                    val speedDrop = recentSpeedDrop()
                    val speedGate = latestSpeedMps >= CRASH_MIN_SPEED_MPS || speedDrop >= CRASH_MIN_SPEED_DROP_MPS
                    if (speedGate && now - lastCrashMs >= CRASH_COOLDOWN_MS) {
                        stageCrashCandidate(
                            now = now,
                            linearAccel = linearAccel,
                            jerk = jerk,
                        )
                    }
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                currentGyroZ = event.values[2]
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun processLocation(location: Location) {
        if (!isDriving) return
        val now = System.currentTimeMillis()
        latestLocation = location
        latestSpeedMps = location.speed.takeIf { it >= 0f } ?: 0f
        DrivingStateStore.setSpeedKmh(latestSpeedMps * 3.6f)

        if (isReliableForMotion(location)) {
            speedHistory.addLast(SpeedPoint(timestampMs = now, speedMps = latestSpeedMps))
            while (speedHistory.isNotEmpty() && now - speedHistory.first().timestampMs > SPEED_HISTORY_WINDOW_MS) {
                speedHistory.removeFirst()
            }

            if (lastSpeedUpdateMs > 0L) {
                val dt = (now - lastSpeedUpdateMs).coerceAtLeast(1L) / 1000f
                val accelMps2 = (latestSpeedMps - lastSpeedMps) / dt
                val motionSpeed = max(latestSpeedMps, lastSpeedMps)
                if (motionSpeed >= MIN_DYNAMIC_EVENT_SPEED_MPS &&
                    accelMps2 <= HARSH_BRAKE_THRESHOLD &&
                    now - lastHarshBrakeMs >= EVENT_COOLDOWN_MS
                ) {
                    lastHarshBrakeMs = now
                    registerRiskEvent("HARSH_BRAKING", abs(accelMps2))
                }
                if (motionSpeed >= MIN_DYNAMIC_EVENT_SPEED_MPS &&
                    accelMps2 >= HARSH_ACCEL_THRESHOLD &&
                    now - lastHarshAccelMs >= EVENT_COOLDOWN_MS
                ) {
                    lastHarshAccelMs = now
                    registerRiskEvent("HARSH_ACCELERATION", accelMps2)
                }
            }

            if (latestSpeedMps >= SPEEDING_THRESHOLD_MPS && now - lastSpeedingMs >= SPEEDING_COOLDOWN_MS) {
                lastSpeedingMs = now
                registerRiskEvent("SPEEDING", latestSpeedMps)
            }
        }

        lastSpeedMps = latestSpeedMps
        lastSpeedUpdateMs = now
        refreshDrivingNotificationIfNeeded()
    }

    private fun registerRiskEvent(type: String, value: Float) {
        val now = System.currentTimeMillis()
        val eventWeight = when (type) {
            "HARSH_BRAKING" -> 22
            "HARSH_ACCELERATION" -> 16
            "SHARP_CORNERING" -> 14
            "SPEEDING" -> 10
            else -> 8
        }
        riskScore = (riskScore + eventWeight).coerceAtMost(100)
        maxRiskScore = max(maxRiskScore, riskScore)
        counters = when (type) {
            "HARSH_BRAKING" -> counters.copy(harshBraking = counters.harshBraking + 1)
            "HARSH_ACCELERATION" -> counters.copy(harshAcceleration = counters.harshAcceleration + 1)
            "SHARP_CORNERING" -> counters.copy(sharpCornering = counters.sharpCornering + 1)
            "SPEEDING" -> counters.copy(speeding = counters.speeding + 1)
            else -> counters
        }

        publishRiskState()
        DrivingStateStore.setLatestEvent(type.replace("_", " "))

        val location = latestLocation
        serviceScope.launch {
            riskEventDao.insert(
                RiskEventEntity(
                    tripId = currentTripId,
                    eventType = type,
                    value = value,
                    speedMps = latestSpeedMps,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    createdAtEpochMs = now,
                ),
            )
        }
    }

    private fun publishRiskState() {
        DrivingStateStore.updateRisk(
            riskScore = riskScore,
            counters = counters,
        )
        refreshDrivingNotificationIfNeeded()
    }

    private fun triggerCrashCountdown(
        simulated: Boolean,
        reason: String,
    ) {
        if (pendingCrash != null) return
        crashValidationJob?.cancel()
        pendingCrashCandidate = null
        pendingCrash = PendingCrash(
            simulated = simulated,
            triggerReason = reason,
            createdAtEpochMs = System.currentTimeMillis(),
        )
        DrivingStateStore.setCrashCountdownActive(true)
        DrivingStateStore.setLatestEvent("Crash gate triggered")
        showCrashAlertNotification()
        launchCrashCountdownActivityBestEffort()
    }

    private fun handleCrashCancelled() {
        val pending = pendingCrash ?: return
        pendingCrash = null
        DrivingStateStore.setCrashCountdownActive(false)
        DrivingStateStore.setLatestEvent("Crash alert cancelled by user")
        DrivingStateStore.setLastAlertOutcome("Cancelled: driver confirmed safe")
        cancelCrashAlertNotification()

        serviceScope.launch {
            crashAlertDao.insert(
                CrashAlertEntity(
                    tripId = currentTripId,
                    createdAtEpochMs = System.currentTimeMillis(),
                    simulated = pending.simulated,
                    outcome = "USER_CANCELLED",
                    smsStatus = "not_sent",
                    callStatus = "not_started",
                    ttsStatus = "not_started",
                    latitude = latestLocation?.latitude,
                    longitude = latestLocation?.longitude,
                ),
            )
        }
    }

    private fun handleCrashEscalation(manualCall: Boolean) {
        val pending = pendingCrash ?: return
        pendingCrash = null
        DrivingStateStore.setCrashCountdownActive(false)
        DrivingStateStore.setLatestEvent("Crash escalation started")
        cancelCrashAlertNotification()

        serviceScope.launch {
            val settings = settingsRepository.settings.value
            val location = latestLocation
            val smsStatus = sendCrashSms(settings.emergencyContacts, location)
            val callStatus = initiateDemoCall(settings.demoCallNumber)
            val ttsStatus = speakIntro(settings.ttsTemplate)
            DrivingStateStore.setLastAlertOutcome("Escalated: SMS $smsStatus, call $callStatus")

            crashAlertDao.insert(
                CrashAlertEntity(
                    tripId = currentTripId,
                    createdAtEpochMs = System.currentTimeMillis(),
                    simulated = pending.simulated,
                    outcome = if (manualCall) "CALL_NOW" else "TIMEOUT_ESCALATION",
                    smsStatus = smsStatus,
                    callStatus = callStatus,
                    ttsStatus = ttsStatus,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                ),
            )
        }
    }

    private fun sendCrashSms(contacts: List<String>, location: Location?): String {
        if (!hasPermission(Manifest.permission.SEND_SMS)) {
            return "permission_denied"
        }
        if (contacts.isEmpty()) return "no_contacts"

        val lat = location?.latitude
        val lon = location?.longitude
        val mapsLink = if (lat != null && lon != null) {
            "https://maps.google.com/?q=$lat,$lon"
        } else {
            "Location unavailable"
        }
        val message = "Project Sentry demo alert: potential crash detected. $mapsLink"

        return try {
            val smsManager = SmsManager.getDefault()
            contacts.forEach { raw ->
                val number = raw.trim()
                if (number.isNotBlank()) {
                    smsManager.sendTextMessage(number, null, message, null, null)
                }
            }
            "sent"
        } catch (e: Exception) {
            "failed:${e.javaClass.simpleName}"
        }
    }

    private suspend fun initiateDemoCall(number: String): String = withContext(Dispatchers.Main) {
        val normalized = number.trim()
        if (normalized.isBlank()) {
            return@withContext "no_number"
        }
        val uri = Uri.parse("tel:$normalized")
        return@withContext try {
            val callIntent = if (hasPermission(Manifest.permission.CALL_PHONE)) {
                Intent(Intent.ACTION_CALL, uri)
            } else {
                Intent(Intent.ACTION_DIAL, uri)
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(callIntent)
            if (hasPermission(Manifest.permission.CALL_PHONE)) "call_intent_started" else "dialer_opened"
        } catch (e: Exception) {
            "failed:${e.javaClass.simpleName}"
        }
    }

    private suspend fun speakIntro(template: String): String {
        val ready = ensureTtsReady()
        if (!ready) return "tts_unavailable"
        val clippedText = template.trim().ifBlank { DEFAULT_TTS }.take(180)
        return withContext(Dispatchers.Main) {
            tts?.speak(
                clippedText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "project_sentry_intro_${System.currentTimeMillis()}",
            )
            "spoken_local_audio"
        }
    }

    private suspend fun ensureTtsReady(): Boolean {
        if (ttsReady && tts != null) return true
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                var created: TextToSpeech? = null
                created = TextToSpeech(applicationContext) { status ->
                    ttsReady = status == TextToSpeech.SUCCESS
                    if (ttsReady) {
                        created?.setSpeechRate(1.0f)
                        tts = created
                    } else {
                        created?.shutdown()
                    }
                    if (cont.isActive) cont.resume(ttsReady)
                }
            }
        }
    }

    private fun recentSpeedDrop(): Float {
        if (speedHistory.size < 2) return 0f
        val maxSpeed = speedHistory.maxOf { it.speedMps }
        val minSpeed = speedHistory.minOf { it.speedMps }
        return (maxSpeed - minSpeed).coerceAtLeast(0f)
    }

    private fun stageCrashCandidate(
        now: Long,
        linearAccel: Float,
        jerk: Float,
    ) {
        val existing = pendingCrashCandidate
        if (existing != null) {
            pendingCrashCandidate = existing.copy(
                peakLinearAccel = max(existing.peakLinearAccel, linearAccel),
                peakJerk = max(existing.peakJerk, jerk),
            )
            return
        }

        val baselineSpeed = currentBaselineSpeedMps()
        pendingCrashCandidate = CrashCandidate(
            detectedAtEpochMs = now,
            baselineSpeedMps = baselineSpeed,
            peakLinearAccel = linearAccel,
            peakJerk = jerk,
        )
        Log.d(
            TAG,
            "Crash candidate staged accel=${"%.1f".format(linearAccel)} jerk=${"%.1f".format(jerk)} baseline=${"%.1f".format(baselineSpeed)}",
        )

        crashValidationJob?.cancel()
        crashValidationJob = serviceScope.launch {
            delay(CRASH_VALIDATION_DELAY_MS)
            evaluatePendingCrashCandidate()
        }
    }

    private fun evaluatePendingCrashCandidate() {
        val candidate = pendingCrashCandidate ?: return
        pendingCrashCandidate = null

        val observedSpeedDrop = max(
            (candidate.baselineSpeedMps - latestSpeedMps).coerceAtLeast(0f),
            recentSpeedDrop(),
        )
        val repeatedImpact = sensorBuffer.countImpactSamplesSince(
            sinceMs = candidate.detectedAtEpochMs - CRASH_SENSOR_LOOKBACK_MS,
            linearAccelThreshold = CRASH_IMPACT_SAMPLE_THRESHOLD,
        ) >= CRASH_IMPACT_SAMPLE_COUNT
        val severeImpact = candidate.peakLinearAccel >= CRASH_SEVERE_LINEAR_ACCEL_THRESHOLD ||
            candidate.peakJerk >= CRASH_SEVERE_JERK_THRESHOLD
        val hardStop = latestSpeedMps <= CRASH_POST_EVENT_SPEED_MAX_MPS &&
            candidate.baselineSpeedMps >= CRASH_MIN_SPEED_MPS

        if ((repeatedImpact || severeImpact) &&
            (observedSpeedDrop >= CRASH_VALIDATED_SPEED_DROP_MPS || hardStop) &&
            System.currentTimeMillis() - lastCrashMs >= CRASH_COOLDOWN_MS
        ) {
            lastCrashMs = System.currentTimeMillis()
            triggerCrashCountdown(
                simulated = false,
                reason = "Validated impact (accel=${"%.1f".format(candidate.peakLinearAccel)}, jerk=${"%.1f".format(candidate.peakJerk)}, speedDrop=${"%.1f".format(observedSpeedDrop)})",
            )
        } else {
            Log.d(
                TAG,
                "Crash candidate ignored accel=${"%.1f".format(candidate.peakLinearAccel)} jerk=${"%.1f".format(candidate.peakJerk)} speedDrop=${"%.1f".format(observedSpeedDrop)} hardStop=$hardStop repeated=$repeatedImpact",
            )
        }
    }

    private fun currentBaselineSpeedMps(): Float {
        return speedHistory.maxOfOrNull { it.speedMps } ?: latestSpeedMps
    }

    private fun isReliableForMotion(location: Location): Boolean {
        val horizontalAccuracyOk = !location.hasAccuracy() || location.accuracy <= MAX_LOCATION_ACCURACY_METERS
        val speedAccuracyOk = !location.hasSpeedAccuracy() ||
            location.speedAccuracyMetersPerSecond <= MAX_SPEED_ACCURACY_MPS
        return horizontalAccuracyOk && speedAccuracyOk
    }

    private fun refreshDrivingNotificationIfNeeded(force: Boolean = false) {
        if (!isDriving) return
        val speedKmh = latestSpeedMps.times(3.6f).toInt()
        val now = System.currentTimeMillis()
        val riskChangedMeaningfully = lastNotificationRisk == Int.MIN_VALUE ||
            abs(riskScore - lastNotificationRisk) >= NOTIFICATION_RISK_DELTA ||
            (riskScore == 0 && lastNotificationRisk != 0)
        val speedChangedMeaningfully = lastNotificationSpeedKmh == Int.MIN_VALUE ||
            abs(speedKmh - lastNotificationSpeedKmh) >= NOTIFICATION_SPEED_DELTA_KMH ||
            (speedKmh == 0 && lastNotificationSpeedKmh > 0)

        if (!force && !riskChangedMeaningfully && !speedChangedMeaningfully) return
        if (!force && now - lastNotificationUpdateMs < NOTIFICATION_MIN_UPDATE_INTERVAL_MS) return

        captureNotificationSnapshot()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildDrivingNotification())
    }

    private fun captureNotificationSnapshot() {
        lastNotificationRisk = riskScore
        lastNotificationSpeedKmh = latestSpeedMps.times(3.6f).toInt()
        lastNotificationUpdateMs = System.currentTimeMillis()
    }

    private fun buildDrivingNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DrivingModeService::class.java).setAction(ACTION_STOP_DRIVING),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = "Risk $riskScore/100 | ${latestSpeedMps.times(3.6f).toInt()} km/h"

        return NotificationCompat.Builder(this, DRIVING_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(getString(R.string.driving_notification_title))
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(0, getString(R.string.stop_driving), stopIntent)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showCrashAlertNotification() {
        val settings = settingsRepository.settings.value
        val launchIntent = buildCrashCountdownIntent(settings.emergencyContacts.size, settings.demoCallNumber)
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            CRASH_ACTIVITY_REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissIntent = PendingIntent.getService(
            this,
            CRASH_CANCEL_REQUEST_CODE,
            Intent(this, DrivingModeService::class.java).setAction(ACTION_CRASH_CANCELLED),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(
            CRASH_NOTIFICATION_ID,
            NotificationCompat.Builder(this, CRASH_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Potential crash detected")
                .setContentText("Countdown started. Confirm safe or escalate now.")
                .setContentIntent(fullScreenIntent)
                .setFullScreenIntent(fullScreenIntent, true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .addAction(0, "I'm OK", dismissIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .build(),
        )
    }

    private fun launchCrashCountdownActivityBestEffort() {
        val settings = settingsRepository.settings.value
        val countdownIntent = buildCrashCountdownIntent(settings.emergencyContacts.size, settings.demoCallNumber)
        try {
            startActivity(countdownIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Crash countdown activity launch was blocked; notification remains available", e)
        }
    }

    private fun buildCrashCountdownIntent(
        contactCount: Int,
        callNumber: String,
    ): Intent {
        return Intent(this, CrashCountdownActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(CrashCountdownActivity.EXTRA_SECONDS, COUNTDOWN_SECONDS)
            putExtra(CrashCountdownActivity.EXTRA_CONTACT_COUNT, contactCount)
            putExtra(CrashCountdownActivity.EXTRA_CALL_NUMBER, callNumber)
        }
    }

    private fun cancelCrashAlertNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(CRASH_NOTIFICATION_ID)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val drivingChannel = NotificationChannel(
                DRIVING_NOTIFICATION_CHANNEL_ID,
                "Driving Status",
                NotificationManager.IMPORTANCE_LOW,
            )
            val crashChannel = NotificationChannel(
                CRASH_NOTIFICATION_CHANNEL_ID,
                "Crash Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            )
            manager.createNotificationChannel(drivingChannel)
            manager.createNotificationChannel(crashChannel)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val ACTION_START_DRIVING = "com.safedrive.ai.action.START_DRIVING"
        const val ACTION_STOP_DRIVING = "com.safedrive.ai.action.STOP_DRIVING"
        const val ACTION_SIMULATE_CRASH = "com.safedrive.ai.action.SIMULATE_CRASH"
        const val ACTION_CRASH_CANCELLED = "com.safedrive.ai.action.CRASH_CANCELLED"
        const val ACTION_CRASH_TIMEOUT = "com.safedrive.ai.action.CRASH_TIMEOUT"
        const val ACTION_CRASH_CALL_NOW = "com.safedrive.ai.action.CRASH_CALL_NOW"

        private const val TAG = "DrivingModeService"
        private const val DRIVING_NOTIFICATION_CHANNEL_ID = "driving_mode_status_v2"
        private const val CRASH_NOTIFICATION_CHANNEL_ID = "crash_alerts_v1"
        private const val NOTIFICATION_ID = 1101
        private const val CRASH_NOTIFICATION_ID = 1102
        private const val CRASH_ACTIVITY_REQUEST_CODE = 201
        private const val CRASH_CANCEL_REQUEST_CODE = 202
        private const val COUNTDOWN_SECONDS = 12

        private const val SENSOR_DELAY_US = 5_000
        private const val EVENT_COOLDOWN_MS = 4_000L
        private const val SPEEDING_COOLDOWN_MS = 8_000L
        private const val SPEED_HISTORY_WINDOW_MS = 5_000L
        private const val CRASH_COOLDOWN_MS = 20_000L
        private const val CRASH_VALIDATION_DELAY_MS = 1_500L
        private const val CRASH_SENSOR_LOOKBACK_MS = 250L
        private const val NOTIFICATION_MIN_UPDATE_INTERVAL_MS = 5_000L
        private const val SENSOR_HEARTBEAT_PUBLISH_MS = 2_000L

        private const val RISK_DECAY_PER_SECOND = 2
        private const val NOTIFICATION_RISK_DELTA = 5
        private const val NOTIFICATION_SPEED_DELTA_KMH = 10
        private const val MAX_LOCATION_ACCURACY_METERS = 25f
        private const val MAX_SPEED_ACCURACY_MPS = 1.8f
        private const val MIN_DYNAMIC_EVENT_SPEED_MPS = 4.5f

        private const val HARSH_BRAKE_THRESHOLD = -4.0f
        private const val HARSH_ACCEL_THRESHOLD = 3.5f
        private const val SHARP_CORNERING_THRESHOLD_RAD = 1.8f
        private const val SHARP_CORNERING_MIN_SPEED_MPS = 8f
        private const val SHARP_CORNERING_MIN_LINEAR_ACCEL = 2.5f
        private const val SPEEDING_THRESHOLD_MPS = 22.2f // ~80 km/h demo threshold

        private const val CRASH_LINEAR_ACCEL_THRESHOLD = 18f
        private const val CRASH_JERK_THRESHOLD = 40f
        private const val CRASH_MIN_SPEED_MPS = 5.5f
        private const val CRASH_MIN_SPEED_DROP_MPS = 4.5f
        private const val CRASH_VALIDATED_SPEED_DROP_MPS = 3.5f
        private const val CRASH_POST_EVENT_SPEED_MAX_MPS = 2.5f
        private const val CRASH_IMPACT_SAMPLE_THRESHOLD = 12f
        private const val CRASH_IMPACT_SAMPLE_COUNT = 2
        private const val CRASH_SEVERE_LINEAR_ACCEL_THRESHOLD = 24f
        private const val CRASH_SEVERE_JERK_THRESHOLD = 65f

        private const val DEFAULT_TTS = "This is a Project Sentry demo alert. Please respond."
    }
}

private data class SpeedPoint(
    val timestampMs: Long,
    val speedMps: Float,
)

private data class PendingCrash(
    val simulated: Boolean,
    val triggerReason: String,
    val createdAtEpochMs: Long,
)

private data class CrashCandidate(
    val detectedAtEpochMs: Long,
    val baselineSpeedMps: Float,
    val peakLinearAccel: Float,
    val peakJerk: Float,
)

private data class SensorSnapshot(
    val timestampMs: Long,
    val ax: Float,
    val ay: Float,
    val az: Float,
    val linearAccel: Float,
    val gyroZ: Float,
    val speedMps: Float,
)

private class CircularSensorBuffer(private val capacity: Int) {
    private val values = arrayOfNulls<SensorSnapshot>(capacity)
    private var index = 0
    private var count = 0

    fun add(sample: SensorSnapshot) {
        values[index] = sample
        index = (index + 1) % capacity
        count = (count + 1).coerceAtMost(capacity)
    }

    fun clear() {
        index = 0
        count = 0
    }

    fun countImpactSamplesSince(
        sinceMs: Long,
        linearAccelThreshold: Float,
    ): Int {
        var total = 0
        repeat(count) { offset ->
            val position = (index - 1 - offset + capacity) % capacity
            val sample = values[position] ?: return@repeat
            if (sample.timestampMs < sinceMs) return@repeat
            if (sample.linearAccel >= linearAccelThreshold) {
                total += 1
            }
        }
        return total
    }
}
