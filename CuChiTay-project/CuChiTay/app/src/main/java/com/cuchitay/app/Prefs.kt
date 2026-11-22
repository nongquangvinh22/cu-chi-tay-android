package com.cuchitay.app

object Prefs {
    const val NAME = "gesture_prefs"

    // Từng thao tác có công tắc độc lập.
    const val SWIPE_UP = "swipe_up"
    const val SWIPE_DOWN = "swipe_down"
    const val SWIPE_LEFT = "swipe_left"
    const val SWIPE_RIGHT = "swipe_right"
    const val GRAB = "grab"
    const val INVERT = "invert"

    // Keys cũ được giữ để chuyển thiết lập từ phiên bản 1.1.
    const val SWIPE = "swipe"
    const val HORIZONTAL = "horizontal"

    const val SENS = "sens"
    const val SCROLL_SPEED = "scroll_speed"
    const val DEFAULT_SCROLL_SPEED = 74
    const val SMOOTHNESS = "smoothness"
    const val COOLDOWN = "cooldown"
    const val DEFAULT_SMOOTHNESS = 65
    const val DEFAULT_COOLDOWN_MS = 700
}
