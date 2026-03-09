package com.availelabs.availe.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.primary
import com.composeunstyled.theme.Theme
import kotlin.math.roundToInt

@Composable
fun LinearSyncIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val translation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clipToBounds()
    ) {
        val barWidthPixels = constraints.maxWidth * 0.4f
        val maxOffsetPixels = constraints.maxWidth + barWidthPixels

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.4f)
                .offset {
                    val currentOffsetPixels = (maxOffsetPixels * translation) - barWidthPixels
                    IntOffset(x = currentOffsetPixels.roundToInt(), y = 0)
                }
                .background(Theme[colors][primary])
        )
    }
}