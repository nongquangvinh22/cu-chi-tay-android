package com.cuchitay.app

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.sqrt

enum class SwipeDir { UP, DOWN, LEFT, RIGHT }

/**
 * Chuyển 21 landmark của MediaPipe thành cử chỉ.
 * Mỗi hướng vuốt được bật/tắt độc lập; thao tác chụp màn hình
 * vẫn là một cử chỉ riêng.
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

    private class Sample(val t: Long, val x: Float, val y: Float)

    private val history = ArrayDeque<Sample>()
    private var smoothX: Float? = null
    private var smoothY: Float? = null
    private var lastSwipeAt = 0L
    private var lastSwipeDir: SwipeDir? = null
    private var lastGrabAt = 0L
    private var lastOpenAt = 0L
    private var fistFrames = 0

    companion object {
        private const val WINDOW_MS = 360L
        private const val MIN_TRAVEL = 0.21f
        private const val MIN_SAMPLE_SPAN_MS = 75L
        private const val SWIPE_COOLDOWN_MS = 430L
        private const val REVERSE_LOCK_MS = 650L
        private const val GRAB_WINDOW_MS = 1100L
        private const val GRAB_COOLDOWN_MS = 2200L
        private const val SMOOTH_ALPHA = 0.72f

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
            fistFrames = 0
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

            if (tipRadius > pipRadius * 1.10f && alignment > 0.18f) {
                extended++
            } else if (tipRadius < pipRadius * 1.05f) {
                folded++
            }
        }

        val isOpen = extended >= 3
        val isFist = extended <= 1 && folded >= 3

        if (isOpen) lastOpenAt = now

        // Xòe tay rồi nắm lại: chụp màn hình.
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

        // Chỉ theo dõi chuyển động khi tay đang xòe.
        if (!isOpen) {
            resetMotion()
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
        cx = 1f - cx // lật gương camera trước

        val filteredX = smoothX?.let { it + SMOOTH_ALPHA * (cx - it) } ?: cx
        val filteredY = smoothY?.let { it + SMOOTH_ALPHA * (cy - it) } ?: cy
        smoothX = filteredX
        smoothY = filteredY

        history.addLast(Sample(now, filteredX, filteredY))
        while (history.isNotEmpty() && now - history.first().t > WINDOW_MS) {
            history.removeFirst()
        }

        if (history.size < 4) return
        if (now - history.first().t < MIN_SAMPLE_SPAN_MS) return
        if (now - lastSwipeAt < SWIPE_COOLDOWN_MS) return

        val first = history.first()
        val dx = (filteredX - first.x) * aspect
        val dy = filteredY - first.y
        val threshold = MIN_TRAVEL / sensitivity.coerceIn(0.6f, 1.6f)

        val detectedDir: SwipeDir? = when {
            abs(dy) >= threshold && abs(dy) > abs(dx) * 1.4f ->
                if (dy < 0f) SwipeDir.UP else SwipeDir.DOWN
            abs(dx) >= threshold && abs(dx) > abs(dy) * 1.4f ->
                if (dx > 0f) SwipeDir.RIGHT else SwipeDir.LEFT
            else -> null
        }

        val detected = detectedDir ?: return
        if (!isDirectionEnabled(detected)) {
            resetMotion()
            return
        }

        val last = lastSwipeDir
        if (last != null && now - lastSwipeAt < REVERSE_LOCK_MS && detected == opposite(last)) {
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

        // Đảo chiều cũng không được vượt qua công tắc độc lập của hướng đích.
        if (!isDirectionEnabled(output)) {
            resetMotion()
            return
        }

        lastSwipeAt = now
        lastSwipeDir = output
        resetMotion()
        onSwipe(output)
    }

    private fun isDirectionEnabled(dir: SwipeDir): Boolean = when (dir) {
        SwipeDir.UP -> swipeUpEnabled
        SwipeDir.DOWN -> swipeDownEnabled
        SwipeDir.LEFT -> swipeLeftEnabled
        SwipeDir.RIGHT -> swipeRightEnabled
    }

    private fun resetMotion() {
        history.clear()
        smoothX = null
        smoothY = null
    }

    private fun opposite(dir: SwipeDir): SwipeDir = when (dir) {
        SwipeDir.UP -> SwipeDir.DOWN
        SwipeDir.DOWN -> SwipeDir.UP
        SwipeDir.LEFT -> SwipeDir.RIGHT
        SwipeDir.RIGHT -> SwipeDir.LEFT
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
