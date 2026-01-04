package com.availelabs.availe.ui.configurations

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

actual object BackGestureConfig {
    // no-op operation
    private val NO_OP_GESTURE_EDGE_WIDTH: Dp = 0.dp
    actual val edgeWidth: Dp = NO_OP_GESTURE_EDGE_WIDTH
    actual val isSwipeProgressRecalculationEnabled: Boolean = false
    actual val minimumDragProgressThreshold: Float = 0.0F
}