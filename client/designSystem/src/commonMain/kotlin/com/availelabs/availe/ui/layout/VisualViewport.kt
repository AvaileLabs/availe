package com.availelabs.availe.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class VisualViewportState(
    val height: Dp = 0.dp,
    val offsetTop: Dp = 0.dp,
    val isResizing: Boolean = false
)

val LocalVisualViewportState = staticCompositionLocalOf { VisualViewportState() }

@Composable
expect fun rememberVisualViewportState(): VisualViewportState