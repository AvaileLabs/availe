package com.availelabs.availe.ui.configurations

import androidx.compose.ui.unit.Dp

expect object BackGestureConfig {
    val edgeWidth: Dp
    val isSwipeProgressRecalculationEnabled: Boolean
    val minimumDragProgressThreshold: Float
}