package com.availelabs.availe.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.availelabs.availe.ui.RefreshUiState
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.uikit.components.Icon
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.onNavigation
import com.composeunstyled.theme.Theme

@Composable
fun RefreshIcon(
    state: RefreshUiState,
    modifier: Modifier = Modifier,
    color: Color = Theme[colors][onNavigation]
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val currentRotation = if (state.isRefreshing) {
        rotation
    } else {
        state.pullProgress * 360f
    }

    Icon(
        imageVector = Lucide.RefreshCw,
        contentDescription = "Refresh",
        modifier = modifier
            .size(24.dp)
            .rotate(currentRotation),
        tint = color
    )
}