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
    private var lastNotificationRisk = 0
    private var lastNotificationSpeedKmh = -1

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
    private var pendingCrash: PendingCrash? = null

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
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DRIVING, null -> startDrivingMode()
            ACTION_STOP_DRIVING -> stopDrivingMode()
            ACTION_SIMULATE_CRASH -> {
                if (!isDriving) startDrivingMode()
                triggerCrashCountdown(
                    simulated = true,
                    reason = "Simulated crash",
                )
            }
            ACTION_CRASH_CANCELLED -> handleCrashCancelled()
            ACTION_CRASH_TIMEOUT -> handleCrashEscalation(manualCall = false)
            ACTION_CRASH_CALL_NOW -> handleCrashEscalation(manualCall = true)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopSensorsAndLocation()
        decayJob?.cancel()
        tts?.shutdown()
        tts = null
        ttsReady = false
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startDrivingMode() {
        if (isDriving) return
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
        pendingCrash = null
        speedHistory.clear()
        sensorBuffer.clear()

        startForeground(NOTIFICATION_ID, buildNotification())
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
        stopSensorsAndLocation()
        decayJob?.cancel()

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
                        gyroZ = currentGyroZ,
                        speedMps = latestSpeedMps,
                    ),
                )

                if (abs(currentGyroZ) >= SHARP_CORNERING_THRESHOLD_RAD &&
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
                        lastCrashMs = now
                        triggerCrashCountdown(
                            simulated = false,
                            reason = "Impact gate (accel=${"%.1f".format(linearAccel)}, jerk=${"%.1f".format(jerk)})",
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

        speedHistory.addLast(SpeedPoint(timestampMs = now, speedMps = latestSpeedMps))
        while (speedHistory.isNotEmpty() && now - speedHistory.first().timestampMs > SPEED_HISTORY_WINDOW_MS) {
            speedHistory.removeFirst()
        }

        if (lastSpeedUpdateMs > 0L) {
            val dt = (now - lastSpeedUpdateMs).coerceAtLeast(1L) / 1000f
            val accelMps2 = (latestSpeedMps - lastSpeedMps) / dt
            if (accelMps2 <= HARSH_BRAKE_THRESHOLD && now - lastHarshBrakeMs >= EVENT_COOLDOWN_MS) {
                lastHarshBrakeMs = now
                registerRiskEvent("HARSH_BRAKING", abs(accelMps2))
            }
            if (accelMps2 >= HARSH_ACCEL_THRESHOLD && now - lastHarshAccelMs >= EVENT_COOLDOWN_MS) {
                lastHarshAccelMs = now
                registerRiskEvent("HARSH_ACCELERATION", accelMps2)
            }
        }

        if (latestSpeedMps >= SPEEDING_THRESHOLD_MPS && now - lastSpeedingMs >= SPEEDING_COOLDOWN_MS) {
            lastSpeedingMs = now
            registerRiskEvent("SPEEDING", latestSpeedMps)
        }

        lastSpeedMps = latestSpeedMps
        lastSpeedUpdateMs = now
        refreshNotificationIfChanged()
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
        refreshNotificationIfChanged()
    }

    private fun triggerCrashCountdown(
        simulated: Boolean,
        reason: String,
    ) {
        if (pendingCrash != null) return
        pendingCrash = PendingCrash(
            simulated = simulated,
            triggerReason = reason,
            createdAtEpochMs = System.currentTimeMillis(),
        )
        DrivingStateStore.setCrashCountdownActive(true)
        DrivingStateStore.setLatestEvent("Crash gate triggered")

        val countdownIntent = Intent(this, CrashCountdownActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(CrashCountdownActivity.EXTRA_SECONDS, COUNTDOWN_SECONDS)
        }
        startActivity(countdownIntent)
    }

    private fun handleCrashCancelled() {
        val pending = pendingCrash ?: return
        pendingCrash = null
        DrivingStateStore.setCrashCountdownActive(false)
        DrivingStateStore.setLatestEvent("Crash alert cancelled by user")

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

        serviceScope.launch {
            val settings = settingsRepository.settings.value
            val location = latestLocation
            val smsStatus = sendCrashSms(settings.emergencyContacts, location)
            val callStatus = initiateDemoCall(settings.demoCallNumber)
            val ttsStatus = speakIntro(settings.ttsTemplate)

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
        val message = "SafeDrive AI demo alert: potential crash detected. $mapsLink"

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
        val normalized = number.trim().ifBlank { "+10000000000" }
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
                "safedrive_intro_${System.currentTimeMillis()}",
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

    private fun refreshNotificationIfChanged() {
        val speedKmh = latestSpeedMps.times(3.6f).toInt()
        if (lastNotificationRisk == riskScore && lastNotificationSpeedKmh == speedKmh) return
        lastNotificationRisk = riskScore
        lastNotificationSpeedKmh = speedKmh
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
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

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(getString(R.string.driving_notification_title))
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(0, getString(R.string.stop_driving), stopIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Driving Mode",
                NotificationManager.IMPORTANCE_HIGH,
            )
            manager.createNotificationChannel(channel)
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

        private const val NOTIFICATION_CHANNEL_ID = "driving_mode"
        private const val NOTIFICATION_ID = 1101
        private const val COUNTDOWN_SECONDS = 12

        private const val SENSOR_DELAY_US = 5_000
        private const val EVENT_COOLDOWN_MS = 2_500L
        private const val SPEEDING_COOLDOWN_MS = 3_000L
        private const val SPEED_HISTORY_WINDOW_MS = 5_000L
        private const val CRASH_COOLDOWN_MS = 20_000L

        private const val RISK_DECAY_PER_SECOND = 2

        private const val HARSH_BRAKE_THRESHOLD = -3.2f
        private const val HARSH_ACCEL_THRESHOLD = 2.8f
        private const val SHARP_CORNERING_THRESHOLD_RAD = 1.4f
        private const val SPEEDING_THRESHOLD_MPS = 22.2f // ~80 km/h demo threshold

        private const val CRASH_LINEAR_ACCEL_THRESHOLD = 18f
        private const val CRASH_JERK_THRESHOLD = 28f
        private const val CRASH_MIN_SPEED_MPS = 5.5f
        private const val CRASH_MIN_SPEED_DROP_MPS = 4.5f

        private const val DEFAULT_TTS = "This is a SafeDrive AI demo alert. Please respond."
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

private data class SensorSnapshot(
    val timestampMs: Long,
    val ax: Float,
    val ay: Float,
    val az: Float,
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
}
