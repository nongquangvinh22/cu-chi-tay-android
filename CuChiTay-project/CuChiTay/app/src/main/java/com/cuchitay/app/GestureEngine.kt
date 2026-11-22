package com.cuchitay.app

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class SwipeDir { UP, DOWN, LEFT, RIGHT }

/**
 * Chuyển 21 landmark của MediaPipe thành cử chỉ.
 * Nhận diện dùng ngưỡng theo kích thước bàn tay, tốc độ, độ thẳng quỹ đạo
 * và chờ tay ổn định trước khi nhận cú hất kế tiếp.
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
    @Volatile var smoothing = 0.48f
    @Volatile var cooldownMs = 700L

    private class Sample(val t: Long, val x: Float, val y: Float, val palmScale: Float)

    private data class Average(
        val first: Float,
        val second: Float,
        val third: Long,
        val fourth: Float
    )

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
        private const val WINDOW_MS = 360L
        private const val MIN_SAMPLE_SPAN_MS = 90L

        private const val REARM_WINDOW_MS = 230L
        private const val REARM_MIN_SPAN_MS = 115L
        private const val REARM_STABLE_MS = 115L

        private const val MIN_FLICK_SPEED = 0.34f
        private const val MIN_DIRECTNESS = 0.50f
        private const val AXIS_DOMINANCE = 1.18f
        private const val FIRST_LAST_AVERAGE = 3

        private const val GRAB_WINDOW_MS = 1100L
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
            resetMotion()
            awaitingRearm = false
            fistFrames = 0
            lastOpenAt = 0L
            return
        }

        var extended = 0
        var folded = 0
        for (finger in FINGERS) {
            val mcp = finger[0]
            val pip = finger[1]
            val dip = finger[2]
            val tip = finger[3]

            val pipRadius = dist(lm[0], lm[pip], aspect).coerceAtLeast(1e-4f)
            val tipRadius = dist(lm[0], lm[tip], aspect)
            val alignment = segmentAlignment(lm[mcp], lm[pip], lm[dip], lm[tip], aspect)

            if (tipRadius > pipRadius * 1.08f && alignment > 0.08f) {
                extended++
            } else if (tipRadius < pipRadius * 1.09f) {
                folded++
            }
        }

        val isOpen = extended >= 3
        val isFist = extended <= 1 && folded >= 3

        if (isOpen) lastOpenAt = now

        // Giữ nguyên thao tác xòe tay rồi nắm lại để chụp màn hình.
        if (grabEnabled) {
            if (isFist) {
                fistFrames++
                if (fistFrames >= 3 &&
                    now - lastOpenAt <= GRAB_WINDOW_MS &&
                    now - lastGrabAt > GRAB_COOLDOWN_MS
                ) {
                    lastGrabAt = now
                    lastOpenAt = 0L
                    resetMotion()
                    onGrab()
                    return
                }
            } else {
                fistFrames = 0
            }
        }

        if (!isOpen) {
            resetMotion()
            awaitingRearm = false
            return
        }

        var cx = 0f
        var cy = 0f
        for (i in PALM) {
            cx += lm[i].x()
            cy += lm[i].y()
        }
        cx /= PALM.size
        cy /= PALM.size
        cx = 1f - cx // Giữ quy ước lật gương camera trước.

        val palmScale = dist(lm[0], lm[9], aspect).coerceIn(0.07f, 0.42f)
        val alpha = smoothing.coerceIn(0.18f, 0.72f)
        val filteredX = smoothX?.let { it + alpha * (cx - it) } ?: cx
        val filteredY = smoothY?.let { it + alpha * (cy - it) } ?: cy
        smoothX = filteredX
        smoothY = filteredY

        history.addLast(Sample(now, filteredX, filteredY, palmScale))

        if (awaitingRearm) {
            while (history.isNotEmpty() && now - history.first().t > REARM_WINDOW_MS) {
                history.removeFirst()
            }
            updateRearm(now, aspect)
            return
        }

        while (history.isNotEmpty() && now - history.first().t > WINDOW_MS) {
            history.removeFirst()
        }

        if (history.size < FIRST_LAST_AVERAGE * 2) return
        if (now - history.first().t < MIN_SAMPLE_SPAN_MS) return
        if (now - lastSwipeAt < cooldownMs) return

        val samples = history.toList()
        val start = average(samples, 0, FIRST_LAST_AVERAGE)
        val end = average(samples, samples.size - FIRST_LAST_AVERAGE, samples.size)

        // Quy đổi hai trục theo cùng tỷ lệ khung hình.
        val dx = (end.first - start.first) * aspect
        val dy = end.second - start.second
        val travelX = abs(dx)
        val travelY = abs(dy)
        val travel = sqrt(dx * dx + dy * dy)

        val sampleSpan = (end.third - start.third).coerceAtLeast(1L)
        val speed = travel * 1000f / sampleSpan.toFloat()

        // Đường đi càng thẳng và dứt khoát thì directness càng gần 1.
        var pathLength = 0f
        for (i in 1 until samples.size) {
            val stepX = (samples[i].x - samples[i - 1].x) * aspect
            val stepY = samples[i].y - samples[i - 1].y
            pathLength += sqrt(stepX * stepX + stepY * stepY)
        }
        val directness = if (pathLength > 1e-4f) travel / pathLength else 0f

        // Ngưỡng co giãn theo kích thước bàn tay và thiết lập độ nhạy.
        val adjustedSensitivity = sensitivity.coerceIn(0.6f, 1.6f)
        val threshold = travelThreshold(end.fourth, adjustedSensitivity)
        val minSpeed = MIN_FLICK_SPEED / adjustedSensitivity

        if (travel < threshold || speed < minSpeed || directness < MIN_DIRECTNESS) return

        val detected: SwipeDir = when {
            travelY >= threshold && travelY > travelX * AXIS_DOMINANCE ->
                if (dy < 0f) SwipeDir.UP else SwipeDir.DOWN
            travelX >= threshold && travelX > travelY * AXIS_DOMINANCE ->
                if (dx > 0f) SwipeDir.RIGHT else SwipeDir.LEFT
            else -> return
        }

        if (!isDirectionEnabled(detected)) {
            resetMotion()
            return
        }

        val output = if (invertVertical) {
            when (detected) {
                SwipeDir.UP -> SwipeDir.DOWN
                SwipeDir.DOWN -> SwipeDir.UP
                else -> detected
            }
        } else {
            detected
        }

        // Đảo chiều vẫn phải tuân theo công tắc độc lập của hướng đích.
        if (!isDirectionEnabled(output)) {
            resetMotion()
            return
        }

        lastSwipeAt = now
        awaitingRearm = true
        stableSince = 0L
        resetHistoryOnly()
        onSwipe(output)
    }

    private fun updateRearm(now: Long, aspect: Float) {
        if (history.size < 4 || now - history.first().t < REARM_MIN_SPAN_MS) {
            stableSince = 0L
            return
        }

        val samples = history.toList()
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (sample in samples) {
            val x = sample.x * aspect
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, sample.y)
            maxY = max(maxY, sample.y)
        }

        val threshold = travelThreshold(samples.last().palmScale, sensitivity)
        val stableLimit = (threshold * 0.24f).coerceIn(0.018f, 0.038f)
        val stable = (maxX - minX) < stableLimit && (maxY - minY) < stableLimit

        if (!stable) {
            stableSince = 0L
            return
        }

        if (stableSince == 0L) stableSince = now
        if (now - stableSince >= REARM_STABLE_MS &&
            now - lastSwipeAt >= cooldownMs
        ) {
            awaitingRearm = false
            resetMotion()
        }
    }

    private fun travelThreshold(palmScale: Float, sensitivityValue: Float): Float {
        return (palmScale * 0.66f / sensitivityValue.coerceIn(0.6f, 1.6f))
            .coerceIn(0.09f, 0.20f)
    }

    private fun average(samples: List<Sample>, from: Int, until: Int): Average {
        var x = 0f
        var y = 0f
        var scale = 0f
        var time = 0L
        var count = 0
        for (i in from until until) {
            val sample = samples[i]
            x += sample.x
            y += sample.y
            scale += sample.palmScale
            time += sample.t
            count++
        }
        val divisor = count.coerceAtLeast(1).toFloat()
        return Average(
            x / divisor,
            y / divisor,
            time / count.coerceAtLeast(1),
            scale / divisor
        )
    }

    private fun isDirectionEnabled(dir: SwipeDir): Boolean = when (dir) {
        SwipeDir.UP -> swipeUpEnabled
        SwipeDir.DOWN -> swipeDownEnabled
        SwipeDir.LEFT -> swipeLeftEnabled
        SwipeDir.RIGHT -> swipeRightEnabled
    }

    private fun resetHistoryOnly() {
        history.clear()
        smoothX = null
        smoothY = null
    }

    private fun resetMotion() {
        resetHistoryOnly()
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

    private fun dist(a: NormalizedLandmark, b: NormalizedLandmark, aspect: Float): Float {
        val dx = (a.x() - b.x()) * aspect
        val dy = a.y() - b.y()
        return sqrt(dx * dx + dy * dy)
    }
}
