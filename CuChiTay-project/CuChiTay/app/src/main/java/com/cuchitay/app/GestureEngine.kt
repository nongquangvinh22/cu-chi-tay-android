package com.cuchitay.app

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.sqrt

enum class SwipeDir { UP, DOWN, LEFT, RIGHT }

/**
 * Biến 21 điểm mốc bàn tay (MediaPipe) thành cử chỉ:
 *  - Hất tay nhanh lên/xuống/trái/phải khi bàn tay đang xòe  -> onSwipe
 *  - Xòe 5 ngón rồi nắm lại                                    -> onGrab
 *
 * Toạ độ landmark đã được xoay thẳng đứng; trục x được lật gương
 * (vì là camera trước) để "tay sang phải" của người dùng = x tăng.
 */
class GestureEngine(
    private val onSwipe: (SwipeDir) -> Unit,
    private val onGrab: () -> Unit
) {
    @Volatile var swipeEnabled = true
    @Volatile var grabEnabled = true
    @Volatile var horizontalEnabled = false
    @Volatile var invertVertical = false
    @Volatile var sensitivity = 1.0f   // lớn hơn = nhạy hơn

    private class Sample(val t: Long, val x: Float, val y: Float)

    private val history = ArrayDeque<Sample>()
    private var lastSwipeAt = 0L
    private var lastSwipeDir: SwipeDir? = null
    private var lastGrabAt = 0L
    private var lastOpenAt = 0L
    private var fistFrames = 0

    companion object {
        private const val WINDOW_MS = 400L          // cửa sổ theo dõi chuyển động
        private const val MIN_TRAVEL = 0.25f        // quãng đường tối thiểu (tính theo chiều cao khung hình)
        private const val SWIPE_COOLDOWN_MS = 700L
        private const val REVERSE_LOCK_MS = 1400L   // chặn cú "thu tay về" bị hiểu là hất ngược
        private const val GRAB_WINDOW_MS = 900L     // từ lúc xòe tay đến lúc nắm phải nhanh hơn mức này
        private const val GRAB_COOLDOWN_MS = 2500L
        private const val EXTENDED_RATIO = 1.2f     // ngón duỗi: đầu ngón xa cổ tay hơn khớp giữa 1.2 lần
        private const val FOLDED_RATIO = 1.0f       // ngón co: đầu ngón gần cổ tay hơn khớp giữa

        // (đầu ngón, khớp giữa) của ngón trỏ, giữa, nhẫn, út
        private val FINGERS = arrayOf(8 to 6, 12 to 10, 16 to 14, 20 to 18)
        // cổ tay + gốc các ngón -> tâm lòng bàn tay
        private val PALM = intArrayOf(0, 5, 9, 13, 17)
    }

    /** lm = null khi không thấy bàn tay nào. */
    fun onHand(lm: List<NormalizedLandmark>?, now: Long, aspect: Float) {
        if (lm == null || lm.size < 21) {
            history.clear()
            fistFrames = 0
            return
        }

        val wrist = lm[0]
        var extended = 0
        var folded = 0
        for ((tip, pip) in FINGERS) {
            val base = dist(wrist, lm[pip], aspect).coerceAtLeast(1e-4f)
            val r = dist(wrist, lm[tip], aspect) / base
            if (r > EXTENDED_RATIO) extended++ else if (r < FOLDED_RATIO) folded++
        }
        val isOpen = extended >= 3
        val isPalm = extended >= 4
        val isFist = extended == 0 && folded >= 3

        if (isPalm) lastOpenAt = now

        // ----- Xòe tay rồi nắm lại: chụp màn hình -----
        if (grabEnabled) {
            if (isFist) {
                fistFrames++
                if (fistFrames >= 2 &&
                    now - lastOpenAt <= GRAB_WINDOW_MS &&
                    now - lastGrabAt > GRAB_COOLDOWN_MS
                ) {
                    lastGrabAt = now
                    lastOpenAt = 0L
                    history.clear()
                    onGrab()
                    return
                }
            } else {
                fistFrames = 0
            }
        }

        // ----- Hất tay: cuộn -----
        if (!swipeEnabled || !isOpen) {
            history.clear()
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

        history.addLast(Sample(now, cx, cy))
        while (history.isNotEmpty() && now - history.first().t > WINDOW_MS) {
            history.removeFirst()
        }
        if (history.size < 3) return
        if (now - lastSwipeAt < SWIPE_COOLDOWN_MS) return

        val first = history.first()
        val dx = (cx - first.x) * aspect // quy về cùng đơn vị với dy
        val dy = cy - first.y
        val threshold = MIN_TRAVEL / sensitivity.coerceIn(0.4f, 2f)

        val dir: SwipeDir = when {
            abs(dy) >= threshold && abs(dy) > abs(dx) * 1.6f ->
                if (dy < 0) SwipeDir.UP else SwipeDir.DOWN
            horizontalEnabled && abs(dx) >= threshold && abs(dx) > abs(dy) * 1.6f ->
                if (dx > 0) SwipeDir.RIGHT else SwipeDir.LEFT
            else -> return
        }

        val last = lastSwipeDir
        if (last != null && now - lastSwipeAt < REVERSE_LOCK_MS && dir == opposite(last)) return

        lastSwipeAt = now
        lastSwipeDir = dir
        history.clear()

        val out = if (invertVertical) {
            when (dir) {
                SwipeDir.UP -> SwipeDir.DOWN
                SwipeDir.DOWN -> SwipeDir.UP
                else -> dir
            }
        } else dir
        onSwipe(out)
    }

    private fun opposite(d: SwipeDir): SwipeDir = when (d) {
        SwipeDir.UP -> SwipeDir.DOWN
        SwipeDir.DOWN -> SwipeDir.UP
        SwipeDir.LEFT -> SwipeDir.RIGHT
        SwipeDir.RIGHT -> SwipeDir.LEFT
    }

    private fun dist(a: NormalizedLandmark, b: NormalizedLandmark, aspect: Float): Float {
        val dx = (a.x() - b.x()) * aspect
        val dy = a.y() - b.y()
        return sqrt(dx * dx + dy * dy)
    }
}
