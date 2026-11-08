package com.cuchitay.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

/**
 * Chỉ làm 2 việc theo lệnh từ GestureService:
 *  - vuốt màn hình (dispatchGesture)
 *  - chụp màn hình (GLOBAL_ACTION_TAKE_SCREENSHOT)
 * Không đọc nội dung màn hình (canRetrieveWindowContent = false).
 */
class GestureAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: GestureAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun swipe(dir: SwipeDir) {
        val dm = resources.displayMetrics
        val w = dm.widthPixels.toFloat()
        val h = dm.heightPixels.toFloat()
        val path = Path()
        when (dir) {
            SwipeDir.UP -> {
                path.moveTo(w * 0.5f, h * 0.75f)
                path.lineTo(w * 0.5f, h * 0.25f)
            }
            SwipeDir.DOWN -> {
                path.moveTo(w * 0.5f, h * 0.25f)
                path.lineTo(w * 0.5f, h * 0.75f)
            }
            SwipeDir.LEFT -> {
                path.moveTo(w * 0.8f, h * 0.5f)
                path.lineTo(w * 0.2f, h * 0.5f)
            }
            SwipeDir.RIGHT -> {
                path.moveTo(w * 0.2f, h * 0.5f)
                path.lineTo(w * 0.8f, h * 0.5f)
            }
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 160L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    fun screenshot() {
        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
    }
}
