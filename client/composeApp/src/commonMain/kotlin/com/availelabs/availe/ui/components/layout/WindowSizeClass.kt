package com.availelabs.availe.ui.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.composeunstyled.currentWindowContainerSize

sealed interface WindowSizeClass {
    data object Compact : WindowSizeClass
    data object Medium : WindowSizeClass
    data object Expanded : WindowSizeClass
}

@Composable
fun rememberWindowSize(): WindowSizeClass {
    val width = currentWindowContainerSize().width
    return when {
        width < 600.dp -> WindowSizeClass.Compact
        width < 1200.dp -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }
}