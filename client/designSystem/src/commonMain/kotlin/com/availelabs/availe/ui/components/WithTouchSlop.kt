package com.availelabs.availe.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun WithTouchSlop(
    touchSlop: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val baseViewConfiguration = LocalViewConfiguration.current
    val density = LocalDensity.current
    val slopPixels = remember(touchSlop, density) { with(density) { touchSlop.toPx() } }

    val patchedViewConfiguration = remember(baseViewConfiguration, slopPixels) {
        object : ViewConfiguration by baseViewConfiguration {
            override val touchSlop: Float = slopPixels
        }
    }

    CompositionLocalProvider(LocalViewConfiguration provides patchedViewConfiguration) {
        content()
    }
}