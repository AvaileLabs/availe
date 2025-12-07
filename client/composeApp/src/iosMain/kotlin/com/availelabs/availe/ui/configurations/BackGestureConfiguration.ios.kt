package com.availelabs.availe.ui.configurations

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

actual object BackGestureConfig {
    private val FULL_SCREEN_GESTURE_WIDTH: Dp = 9999.dp
    actual val edgeWidth: Dp = FULL_SCREEN_GESTURE_WIDTH
    actual val isSwipeProgressRecalculationEnabled: Boolean = true
}