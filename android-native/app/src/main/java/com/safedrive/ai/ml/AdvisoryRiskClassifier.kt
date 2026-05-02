package com.safedrive.ai.ml

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sqrt
import org.json.JSONArray
import org.json.JSONObject

data class AdvisorySensorSample(
    val timestampMs: Long,
    val linearAccel: Float,
    val jerk: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
)

data class AdvisoryRiskResult(
    val label: String,
    val riskScore: Int,
    val confidence: Float,
    val source: String,
)

class AdvisoryRiskClassifier(context: Context) {
    private val appContext = context.applicationContext
    private val featureWindow = AdvisoryFeatureWindow()
    private val smoother = AdvisoryRiskSmoother()
    private val loadingStarted = AtomicBoolean(false)

    @Volatile
    private var model: AdvisoryRiskModel = BaselineAdvisoryRiskModel

    fun reset() {
        featureWindow.clear()
        smoother.clear()
        warmUp()
    }

    fun addSample(sample: AdvisorySensorSample) {
        featureWindow.add(sample)
    }

    fun classifyIfReady(nowMs: Long): AdvisoryRiskResult? {
        val features = featureWindow.features(nowMs) ?: return null
        warmUp()
        return smoother.add(model.classify(features))
    }

    fun warmUp() {
        if (!loadingStarted.compareAndSet(false, true)) return
        Thread {
            model = try {
                AdvisoryRiskModel.load(appContext)
            } catch (_: Throwable) {
                BaselineAdvisoryRiskModel
            }
        }.apply {
            name = "sentry-advisory-risk-loader"
            isDaemon = true
            start()
        }
    }
}

private class AdvisoryFeatureWindow {
    private val samples = ArrayDeque<AdvisorySensorSample>()

    fun add(sample: AdvisorySensorSample) {
        samples.addLast(sample)
        trim(sample.timestampMs)
    }

    fun clear() {
        samples.clear()
    }

    fun features(nowMs: Long): Map<String, Float>? {
        trim(nowMs)
        if (samples.size < MIN_WINDOW_SAMPLES) return null
        val durationMs = samples.last().timestampMs - samples.first().timestampMs
        if (durationMs < MIN_WINDOW_MS) return null

        val accel = samples.map { it.linearAccel.coerceAtLeast(0f) }
        val jerk = samples.map { it.jerk.coerceAtLeast(0f) }
        val gyro = samples.map {
            sqrt(
                it.gyroX * it.gyroX +
                    it.gyroY * it.gyroY +
                    it.gyroZ * it.gyroZ,
            )
        }
        val motion = accel.zip(gyro) { a, g -> a + g }

        return mapOf(
            *signalFeatures("accel", accel, peakThreshold = 2.5f).toList().toTypedArray(),
            *signalFeatures("jerk", jerk, peakThreshold = 15f, includeSma = false, percentiles = listOf(0.90f, 0.95f)).toList().toTypedArray(),
            *signalFeatures("gyro", gyro, peakThreshold = 0.75f, percentiles = listOf(0.90f, 0.95f)).toList().toTypedArray(),
            *compactSignalFeatures("motion", motion).toList().toTypedArray(),
        )
    }

    private fun trim(nowMs: Long) {
        while (samples.isNotEmpty() && nowMs - samples.first().timestampMs > WINDOW_MS) {
            samples.removeFirst()
        }
    }

    private companion object {
        const val WINDOW_MS = 3_000L
        const val MIN_WINDOW_MS = 2_000L
        const val MIN_WINDOW_SAMPLES = 40
    }
}

private class AdvisoryRiskSmoother {
    private val history = ArrayDeque<AdvisoryRiskResult>()
    private var stableLabel: String? = null

    fun clear() {
        history.clear()
        stableLabel = null
    }

    fun add(raw: AdvisoryRiskResult): AdvisoryRiskResult {
        history.addLast(raw)
        while (history.size > HISTORY_SIZE) {
            history.removeFirst()
        }

        val voteLabel = history
            .groupingBy { it.label }
            .eachCount()
            .maxWithOrNull(compareBy<Map.Entry<String, Int>> { it.value }.thenBy { averageConfidence(it.key) })
            ?.key
            ?: raw.label
        val voteConfidence = averageConfidence(voteLabel)
        val current = stableLabel
        if (current == null ||
            voteLabel == current ||
            (voteConfidence >= LABEL_CHANGE_CONFIDENCE && history.count { it.label == voteLabel } >= LABEL_CHANGE_VOTES)
        ) {
            stableLabel = voteLabel
        }

        val label = stableLabel ?: voteLabel
        val labelHistory = history.filter { it.label == label }.ifEmpty { history.toList() }
        val score = labelHistory.map { it.riskScore }.average().toInt().coerceIn(0, 100)
        val confidence = labelHistory.map { it.confidence }.average().toFloat().coerceIn(0f, 1f)
        val source = if (DEMO_ASSIST_MODE) {
            "${raw.source}:demo-assist"
        } else {
            raw.source
        }

        return AdvisoryRiskResult(
            label = label,
            riskScore = score,
            confidence = confidence,
            source = source,
        )
    }

    private fun averageConfidence(label: String): Float {
        val matching = history.filter { it.label == label }
        if (matching.isEmpty()) return 0f
        return matching.map { it.confidence }.average().toFloat()
    }

    private companion object {
        const val HISTORY_SIZE = 5
        const val LABEL_CHANGE_CONFIDENCE = 0.48f
        const val LABEL_CHANGE_VOTES = 2
        const val DEMO_ASSIST_MODE = false
    }
}

private interface AdvisoryRiskModel {
    val source: String

    fun classify(features: Map<String, Float>): AdvisoryRiskResult

    companion object {
        fun load(context: Context): AdvisoryRiskModel {
            loadTwoTask(context)?.let { return it }
            return loadSingleAsset(context, ANDROID_MODEL_FILE) ?: BaselineAdvisoryRiskModel
        }

        private fun loadTwoTask(context: Context): AdvisoryRiskModel? {
            val driver = loadSingleAsset(context, ANDROID_DRIVER_MODEL_FILE) ?: return null
            val road = loadSingleAsset(context, ANDROID_ROAD_MODEL_FILE) ?: return null
            return TwoTaskAdvisoryRiskModel(driverModel = driver, roadModel = road)
        }

        private fun loadSingleAsset(context: Context, fileName: String): AdvisoryRiskModel? {
            return try {
                val text = context.assets.open(fileName).bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                when (json.optString("model_type", "logistic_regression")) {
                    "random_forest" -> RandomForestAdvisoryRiskModel.fromJson(json)
                    "extra_trees" -> RandomForestAdvisoryRiskModel.fromJson(json)
                    else -> LogisticRegressionAdvisoryRiskModel.fromJson(json)
                }
            } catch (_: Exception) {
                null
            }
        }

        private const val ANDROID_MODEL_FILE = "advisory_risk_model.json"
        private const val ANDROID_DRIVER_MODEL_FILE = "advisory_driver_model.json"
        private const val ANDROID_ROAD_MODEL_FILE = "advisory_road_model.json"
    }
}

private class TwoTaskAdvisoryRiskModel(
    private val driverModel: AdvisoryRiskModel,
    private val roadModel: AdvisoryRiskModel,
) : AdvisoryRiskModel {
    override val source: String = "trained-json:two-task"

    override fun classify(features: Map<String, Float>): AdvisoryRiskResult {
        val driver = driverModel.classify(features)
        val road = roadModel.classify(features)
        return if (road.label == "ROAD_ANOMALY" && road.confidence >= ROAD_ANOMALY_CONFIDENCE) {
            road.copy(source = source)
        } else {
            driver.copy(source = source)
        }
    }

    private companion object {
        const val ROAD_ANOMALY_CONFIDENCE = 0.55f
    }
}

private class LogisticRegressionAdvisoryRiskModel(
    private val featureNames: List<String>,
    private val classes: List<String>,
    private val mean: FloatArray,
    private val scale: FloatArray,
    private val coefficients: List<FloatArray>,
    private val intercepts: FloatArray,
    private val scoreRanges: Map<String, IntRange>,
) : AdvisoryRiskModel {
    override val source: String = "trained-json"

    override fun classify(features: Map<String, Float>): AdvisoryRiskResult {
        val normalized = FloatArray(featureNames.size) { index ->
            val raw = features[featureNames[index]] ?: 0f
            val divisor = if (abs(scale[index]) < 0.000001f) 1f else scale[index]
            (raw - mean[index]) / divisor
        }

        val probabilities = if (classes.size == 2 && coefficients.size == 1 && intercepts.size == 1) {
            val positiveProbability = sigmoid(logit(coefficients[0], intercepts[0], normalized))
            floatArrayOf(1f - positiveProbability, positiveProbability)
        } else {
            val logits = FloatArray(classes.size) { classIndex ->
                logit(coefficients[classIndex], intercepts[classIndex], normalized)
            }
            softmax(logits)
        }
        val bestIndex = probabilities.indices.maxBy { probabilities[it] }
        val label = classes[bestIndex]
        val confidence = probabilities[bestIndex]

        return AdvisoryRiskResult(
            label = label,
            riskScore = scoreFor(label, confidence),
            confidence = confidence,
            source = source,
        )
    }

    private fun scoreFor(label: String, confidence: Float): Int {
        val range = scoreRanges[label] ?: defaultRange(label)
        val confidenceInRange = confidence.coerceIn(0f, 1f)
        return (range.first + ((range.last - range.first) * confidenceInRange)).toInt().coerceIn(0, 100)
    }

    private fun defaultRange(label: String): IntRange {
        return when (label) {
            "ROAD_ANOMALY" -> 60..85
            "RISKY" -> 70..100
            "AGGRESSIVE" -> 40..70
            else -> 0..30
        }
    }

    private fun logit(coefficientRow: FloatArray, intercept: Float, normalized: FloatArray): Float {
        var value = intercept
        for (featureIndex in normalized.indices) {
            value += coefficientRow[featureIndex] * normalized[featureIndex]
        }
        return value
    }

    companion object {
        fun fromJson(json: JSONObject): LogisticRegressionAdvisoryRiskModel {
            return LogisticRegressionAdvisoryRiskModel(
                featureNames = json.getJSONArray("feature_names").toStringList(),
                classes = json.getJSONArray("classes").toStringList().map { it.uppercase() },
                mean = json.getJSONObject("scaler").getJSONArray("mean").toFloatArray(),
                scale = json.getJSONObject("scaler").getJSONArray("scale").toFloatArray(),
                coefficients = json.getJSONArray("coefficients").toFloatMatrix(),
                intercepts = json.getJSONArray("intercepts").toFloatArray(),
                scoreRanges = json.getJSONObject("score_ranges").toScoreRanges(),
            )
        }
    }
}

private object BaselineAdvisoryRiskModel : AdvisoryRiskModel {
    override val source: String = "baseline"

    override fun classify(features: Map<String, Float>): AdvisoryRiskResult {
        val accelP95 = features["accel_p95"] ?: 0f
        val accelMax = features["accel_max"] ?: 0f
        val jerkMax = features["jerk_max"] ?: 0f
        val gyroP95 = features["gyro_p95"] ?: 0f
        val motionMax = features["motion_max"] ?: 0f

        val score = (
            scaled(accelP95, low = 0.8f, high = 7.0f, weight = 32f) +
                scaled(accelMax, low = 2.0f, high = 12.0f, weight = 22f) +
                scaled(jerkMax, low = 8.0f, high = 55.0f, weight = 24f) +
                scaled(gyroP95, low = 0.4f, high = 2.8f, weight = 14f) +
                scaled(motionMax, low = 3.0f, high = 14.0f, weight = 8f)
            ).toInt().coerceIn(0, 100)

        val label = when {
            score >= 70 -> "RISKY"
            score >= 40 -> "AGGRESSIVE"
            else -> "NORMAL"
        }
        val nearestBoundaryDistance = min(abs(score - 40), abs(score - 70))
        val confidence = (0.56f + nearestBoundaryDistance / 120f).coerceIn(0.56f, 0.90f)

        return AdvisoryRiskResult(
            label = label,
            riskScore = score,
            confidence = confidence,
            source = source,
        )
    }

    private fun scaled(value: Float, low: Float, high: Float, weight: Float): Float {
        if (high <= low) return 0f
        return (((value - low) / (high - low)).coerceIn(0f, 1f) * weight)
    }
}

private class RandomForestAdvisoryRiskModel(
    private val featureNames: List<String>,
    private val classes: List<String>,
    private val trees: List<AdvisoryDecisionTree>,
    private val scoreRanges: Map<String, IntRange>,
) : AdvisoryRiskModel {
    override val source: String = "trained-json"

    override fun classify(features: Map<String, Float>): AdvisoryRiskResult {
        val featureValues = FloatArray(featureNames.size) { index ->
            features[featureNames[index]] ?: 0f
        }
        val probabilities = FloatArray(classes.size)
        trees.forEach { tree ->
            val treeProbabilities = tree.predict(featureValues)
            for (index in probabilities.indices) {
                probabilities[index] += treeProbabilities[index] / trees.size.toFloat()
            }
        }
        val bestIndex = probabilities.indices.maxBy { probabilities[it] }
        val label = classes[bestIndex]
        val confidence = probabilities[bestIndex].coerceIn(0f, 1f)

        return AdvisoryRiskResult(
            label = label,
            riskScore = scoreFor(label, confidence),
            confidence = confidence,
            source = source,
        )
    }

    private fun scoreFor(label: String, confidence: Float): Int {
        val range = scoreRanges[label] ?: defaultRange(label)
        val confidenceInRange = confidence.coerceIn(0f, 1f)
        return (range.first + ((range.last - range.first) * confidenceInRange)).toInt().coerceIn(0, 100)
    }

    private fun defaultRange(label: String): IntRange {
        return when (label) {
            "ROAD_ANOMALY" -> 60..85
            "RISKY" -> 70..100
            "AGGRESSIVE" -> 40..70
            else -> 0..30
        }
    }

    companion object {
        fun fromJson(json: JSONObject): RandomForestAdvisoryRiskModel {
            return RandomForestAdvisoryRiskModel(
                featureNames = json.getJSONArray("feature_names").toStringList(),
                classes = json.getJSONArray("classes").toStringList().map { it.uppercase() },
                trees = json.getJSONArray("trees").toTrees(),
                scoreRanges = json.getJSONObject("score_ranges").toScoreRanges(),
            )
        }
    }
}

private data class AdvisoryDecisionTree(
    private val childrenLeft: IntArray,
    private val childrenRight: IntArray,
    private val feature: IntArray,
    private val threshold: FloatArray,
    private val probabilities: List<FloatArray>,
) {
    fun predict(features: FloatArray): FloatArray {
        var node = 0
        while (childrenLeft[node] != -1 && childrenRight[node] != -1) {
            val featureIndex = feature[node]
            node = if (featureIndex >= 0 && features[featureIndex] <= threshold[node]) {
                childrenLeft[node]
            } else {
                childrenRight[node]
            }
        }
        return probabilities[node]
    }
}

private fun softmax(values: FloatArray): FloatArray {
    val maxValue = values.maxOrNull() ?: 0f
    val exps = values.map { exp((it - maxValue).toDouble()).toFloat() }
    val total = exps.sum().coerceAtLeast(0.000001f)
    return FloatArray(exps.size) { index -> exps[index] / total }
}

private fun sigmoid(value: Float): Float {
    return (1.0 / (1.0 + exp(-value.toDouble()))).toFloat()
}

private fun List<Float>.mean(): Float {
    if (isEmpty()) return 0f
    return sum() / size
}

private fun List<Float>.std(): Float {
    if (isEmpty()) return 0f
    val average = mean()
    val variance = fold(0f) { total, value -> total + (value - average) * (value - average) } / size
    return sqrt(variance)
}

private fun List<Float>.maxOrZero(): Float = maxOrNull() ?: 0f

private fun List<Float>.minOrZero(): Float = minOrNull() ?: 0f

private fun List<Float>.rms(): Float {
    if (isEmpty()) return 0f
    return sqrt(map { it * it }.mean())
}

private fun List<Float>.energy(): Float {
    if (isEmpty()) return 0f
    return map { it * it }.mean()
}

private fun List<Float>.percentile(percentile: Float): Float {
    if (isEmpty()) return 0f
    val sorted = sorted()
    val rank = (sorted.size - 1) * percentile.coerceIn(0f, 1f)
    val lower = rank.toInt().coerceIn(0, sorted.lastIndex)
    val upper = (lower + 1).coerceIn(0, sorted.lastIndex)
    val fraction = rank - lower
    return sorted[lower] + (sorted[upper] - sorted[lower]) * fraction
}

private fun signalFeatures(
    prefix: String,
    values: List<Float>,
    peakThreshold: Float,
    includeSma: Boolean = true,
    percentiles: List<Float> = listOf(0.50f, 0.75f, 0.90f, 0.95f),
): Map<String, Float> {
    val result = linkedMapOf(
        "${prefix}_mean" to values.mean(),
        "${prefix}_std" to values.std(),
        "${prefix}_min" to values.minOrZero(),
        "${prefix}_max" to values.maxOrZero(),
        "${prefix}_rms" to values.rms(),
        "${prefix}_energy" to values.energy(),
        "${prefix}_peak_count" to values.count { it >= peakThreshold }.toFloat(),
    )
    percentiles.forEach { percentile ->
        result["${prefix}_p${(percentile * 100).toInt()}"] = values.percentile(percentile)
    }
    if (includeSma) {
        result["${prefix}_sma"] = values.map { abs(it) }.mean()
    }
    return result
}

private fun compactSignalFeatures(prefix: String, values: List<Float>): Map<String, Float> {
    return mapOf(
        "${prefix}_mean" to values.mean(),
        "${prefix}_std" to values.std(),
        "${prefix}_max" to values.maxOrZero(),
        "${prefix}_rms" to values.rms(),
        "${prefix}_energy" to values.energy(),
    )
}

private fun JSONArray.toStringList(): List<String> {
    return List(length()) { index -> getString(index) }
}

private fun JSONArray.toFloatArray(): FloatArray {
    return FloatArray(length()) { index -> getDouble(index).toFloat() }
}

private fun JSONArray.toIntArray(): IntArray {
    return IntArray(length()) { index -> getInt(index) }
}

private fun JSONArray.toFloatMatrix(): List<FloatArray> {
    return List(length()) { index -> getJSONArray(index).toFloatArray() }
}

private fun JSONArray.toTrees(): List<AdvisoryDecisionTree> {
    return List(length()) { index ->
        val tree = getJSONObject(index)
        AdvisoryDecisionTree(
            childrenLeft = tree.getJSONArray("children_left").toIntArray(),
            childrenRight = tree.getJSONArray("children_right").toIntArray(),
            feature = tree.getJSONArray("feature").toIntArray(),
            threshold = tree.getJSONArray("threshold").toFloatArray(),
            probabilities = tree.getJSONArray("probabilities").toFloatMatrix(),
        )
    }
}

private fun JSONObject.toScoreRanges(): Map<String, IntRange> {
    return keys().asSequence().associate { key ->
        val values = getJSONArray(key)
        key.uppercase() to values.getInt(0)..values.getInt(1)
    }
}
