package com.cuchitay.app

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class SwipeDir { UP, DOWN, LEFT, RIGHT }

/**
 * Robust palm-motion classifier for MediaPipe's 21 hand landmarks.
 * It accepts a naturally relaxed open hand, measures a short directional flick,
 * and waits for the hand to settle before allowing the next swipe.
 */
class GestureEngine(
    private val onSwipe: (SwipeDir) -> Unit,
    private val onGrab: () -> Unit
) {
    @Volatile var swipeUpEnabled = true
    @Volatile var swipeDownEnabled = true
    @Volatile var swipeLeftEnabled = false
    @Volatile var swipeRightEnabled = false
    @Volatile var grabEnabled = true
    @Volatile var invertVertical = false
    @Volatile var sensitivity = 1.0f
    /** Low alpha means more smoothing. */
    @Volatile var smoothing = 0.40f
    @Volatile var cooldownMs = 700L

    private data class Sample(val t: Long, val x: Float, val y: Float, val palmScale: Float)
    private data class Point(val x: Float, val y: Float)

    private val history = ArrayDeque<Sample>()
    private var smoothX: Float? = null
    private var smoothY: Float? = null
    private var lastSwipeAt = 0L
    private var lastGrabAt = 0L
    private var lastOpenAt = 0L
    private var fistFrames = 0
    private var awaitingRearm = false
    private var stableSince = 0L

    companion object {
        private const val MOTION_WINDOW_MS = 280L
        private const val MIN_SAMPLE_SPAN_MS = 75L
        private const val REARM_WINDOW_MS = 320L
        private const val REARM_MIN_SPAN_MS = 150L
        private const val REARM_STABLE_MS = 145L
        private const val MIN_FLICK_SPEED = 0.34f
        private const val MIN_DIRECTNESS = 0.42f
        private const val AXIS_DOMINANCE = 1.16f
        private const val GROUP_SIZE = 2
        private const val GRAB_WINDOW_MS = 1300L
        private const val GRAB_COOLDOWN_MS = 2200L

        private val FINGERS = arrayOf(
            intArrayOf(5, 6, 7, 8),
            intArrayOf(9, 10, 11, 12),
            intArrayOf(13, 14, 15, 16),
            intArrayOf(17, 18, 19, 20)
        )
        private val PALM = intArrayOf(0, 5, 9, 13, 17)
    }

    fun onHand(lm: List<NormalizedLandmark>?, now: Long, aspect: Float) {
        if (lm == null || lm.size < 21) {
            resetAllMotion()
            awaitingRearm = false
            fistFrames = 0
            lastOpenAt = 0L
            return
        }

        val extended = countExtendedFingers(lm, aspect)
        val folded = 4 - extended
        val isOpen = extended >= 3
        val isFist = extended <= 1 && folded >= 3
        val isSwipePose = extended >= 2 && !isFist

        if (isOpen) lastOpenAt = now
        if (grabEnabled) {
            if (isFist) {
                fistFrames++
                if (fistFrames >= 3 && now - lastOpenAt in 0..GRAB_WINDOW_MS &&
                    now - lastGrabAt > GRAB_COOLDOWN_MS) {
                    lastGrabAt = now
                    lastOpenAt = 0L
                    resetAllMotion()
                    awaitingRearm = false
                    onGrab()
                    return
                }
            } else fistFrames = 0
        } else fistFrames = 0

        // Permit a hand with one slightly curled finger; reject a closed fist.
        if (!isSwipePose) {
            resetAllMotion()
            awaitingRearm = false
            return
        }

        val centre = palmCentre(lm)
        val smoothAlpha = smoothing.coerceIn(0.18f, 0.72f)
        val x = smoothX?.let { it + smoothAlpha * (centre.x - it) } ?: centre.x
        val y = smoothY?.let { it + smoothAlpha * (centre.y - it) } ?: centre.y
        smoothX = x
        smoothY = y

        val palmScale = distance(lm[0], lm[9], aspect).coerceIn(0.07f, 0.42f)
        history.addLast(Sample(now, x, y, palmScale))

        if (awaitingRearm) {
            trimHistory(now, REARM_WINDOW_MS)
            updateRearm(now, aspect)
            return
        }

        trimHistory(now, MOTION_WINDOW_MS)
        if (history.size < GROUP_SIZE * 2 + 1) return
        if (now - history.first().t < MIN_SAMPLE_SPAN_MS) return
        if (now - lastSwipeAt < cooldownMs.coerceIn(350L, 1400L)) return

        val samples = history.toList()
        val start = average(samples.take(GROUP_SIZE))
        val end = average(samples.takeLast(GROUP_SIZE))
        val safeAspect = aspect.takeIf { it.isFinite() && it in 0.45f..2.2f } ?: 1f
        val dx = (end.x - start.x) * safeAspect
        val dy = end.y - start.y
        val travelX = abs(dx)
        val travelY = abs(dy)
        val travel = sqrt(dx * dx + dy * dy)
        val span = (end.t - start.t).coerceAtLeast(1L)
        val speed = travel * 1000f / span.toFloat()

        var pathLength = 0f
        for (i in 1 until samples.size) {
            val sx = (samples[i].x - samples[i - 1].x) * safeAspect
            val sy = samples[i].y - samples[i - 1].y
            pathLength += sqrt(sx * sx + sy * sy)
        }
        val directness = if (pathLength > 1e-4f) travel / pathLength else 0f
        val threshold = travelThreshold(end.palmScale, sensitivity)
        val minSpeed = MIN_FLICK_SPEED / sensitivity.coerceIn(0.6f, 1.6f)
        if (span !in MIN_SAMPLE_SPAN_MS..MOTION_WINDOW_MS ||
            travel < threshold || speed < minSpeed || directness < MIN_DIRECTNESS) return

        val detected = when {
            travelX >= threshold && travelX > travelY * AXIS_DOMINANCE ->
                if (dx < 0f) SwipeDir.LEFT else SwipeDir.RIGHT
            travelY >= threshold && travelY > travelX * AXIS_DOMINANCE ->
                if (dy < 0f) SwipeDir.UP else SwipeDir.DOWN
            else -> return
        }
        val output = if (invertVertical) {
            when (detected) {
                SwipeDir.UP -> SwipeDir.DOWN
                SwipeDir.DOWN -> SwipeDir.UP
                else -> detected
            }
        } else detected

        // Check the final action so reverse-scroll can independently be enabled.
        if (!isDirectionEnabled(output)) {
            clearCandidate()
            return
        }

        lastSwipeAt = now
        awaitingRearm = true
        stableSince = 0L
        history.clear()
        onSwipe(output)
    }

    private fun countExtendedFingers(lm: List<NormalizedLandmark>, aspect: Float): Int {
        var extended = 0
        for (finger in FINGERS) {
            val mcp = finger[0]
            val pip = finger[1]
            val dip = finger[2]
            val tip = finger[3]
            val pipRadius = distance(lm[0], lm[pip], aspect).coerceAtLeast(1e-4f)
            val tipRadius = distance(lm[0], lm[tip], aspect)
            val alignment = segmentAlignment(lm[mcp], lm[pip], lm[dip], lm[tip], aspect)
            if (tipRadius > pipRadius * 1.07f && alignment > 0.02f) extended++
        }
        return extended
    }

    private fun palmCentre(lm: List<NormalizedLandmark>): Point {
        var x = 0f
        var y = 0f
        for (i in PALM) {
            x += lm[i].x()
            y += lm[i].y()
        }
        val count = PALM.size.toFloat()
        // Mirror the front-camera x axis so left/right match the selfie preview.
        return Point(1f - x / count, y / count)
    }

    private fun updateRearm(now: Long, aspect: Float) {
        if (history.size < 5 || now - history.first().t < REARM_MIN_SPAN_MS) {
            stableSince = 0L
            return
        }
        val safeAspect = aspect.takeIf { it.isFinite() && it in 0.45f..2.2f } ?: 1f
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (sample in history) {
            val x = sample.x * safeAspect
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, sample.y)
            maxY = max(maxY, sample.y)
        }
        val stableLimit = (travelThreshold(history.last().palmScale, sensitivity) * 0.28f)
            .coerceIn(0.018f, 0.042f)
        val stable = (maxX - minX) < stableLimit && (maxY - minY) < stableLimit
        if (!stable) {
            stableSince = 0L
            return
        }
        if (stableSince == 0L) stableSince = now
        if (now - stableSince >= REARM_STABLE_MS &&
            now - lastSwipeAt >= cooldownMs.coerceIn(350L, 1400L)) {
            awaitingRearm = false
            resetAllMotion()
        }
    }

    private fun trimHistory(now: Long, windowMs: Long) {
        while (history.isNotEmpty() && now - history.first().t > windowMs) history.removeFirst()
    }

    private fun average(samples: List<Sample>): Sample {
        val count = samples.size.coerceAtLeast(1).toFloat()
        return Sample(
            samples.sumOf { it.x.toDouble() }.toFloat() / count,
            samples.sumOf { it.y.toDouble() }.toFloat() / count,
            samples.sumOf { it.t } / samples.size.coerceAtLeast(1),
            samples.sumOf { it.palmScale.toDouble() }.toFloat() / count
        )
    }

    private fun travelThreshold(palmScale: Float, sensitivityValue: Float): Float =
        (palmScale * 0.58f / sensitivityValue.coerceIn(0.6f, 1.6f)).coerceIn(0.075f, 0.155f)

    private fun isDirectionEnabled(dir: SwipeDir): Boolean = when (dir) {
        SwipeDir.UP -> swipeUpEnabled
        SwipeDir.DOWN -> swipeDownEnabled
        SwipeDir.LEFT -> swipeLeftEnabled
        SwipeDir.RIGHT -> swipeRightEnabled
    }

    private fun clearCandidate() {
        history.clear()
        stableSince = 0L
    }

    private fun resetAllMotion() {
        history.clear()
        smoothX = null
        smoothY = null
        stableSince = 0L
    }

    private fun segmentAlignment(
        mcp: NormalizedLandmark,
        pip: NormalizedLandmark,
        dip: NormalizedLandmark,
        tip: NormalizedLandmark,
        aspect: Float
    ): Float {
        val ax = (pip.x() - mcp.x()) * aspect
        val ay = pip.y() - mcp.y()
        val bx = (tip.x() - dip.x()) * aspect
        val by = tip.y() - dip.y()
        val aLength = sqrt(ax * ax + ay * ay).coerceAtLeast(1e-5f)
        val bLength = sqrt(bx * bx + by * by).coerceAtLeast(1e-5f)
        return ((ax * bx + ay * by) / (aLength * bLength)).coerceIn(-1f, 1f)
    }

    private fun distance(a: NormalizedLandmark, b: NormalizedLandmark, aspect: Float): Float {
        val dx = (a.x() - b.x()) * aspect
        val dy = a.y() - b.y()
        return sqrt(dx * dx + dy * dy)
    }
}
