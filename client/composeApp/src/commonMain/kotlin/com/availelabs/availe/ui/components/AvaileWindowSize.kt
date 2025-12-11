package com.availelabs.availe.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.composeunstyled.currentWindowContainerSize

sealed interface WindowSize {
    data object Compact : WindowSize
    data object Medium : WindowSize
    data object Expanded : WindowSize
}

@Composable
fun rememberWindowSize(): WindowSize {
    val width = currentWindowContainerSize().width
    return when {
        width < 600.dp -> WindowSize.Compact
        width < 1200.dp -> WindowSize.Medium
        else -> WindowSize.Expanded
    }
}