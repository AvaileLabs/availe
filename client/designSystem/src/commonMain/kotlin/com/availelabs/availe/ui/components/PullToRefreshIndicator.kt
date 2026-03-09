package com.availelabs.availe.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.accessibility_pull_to_refresh
import com.composables.icons.lucide.ArrowDown
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.Icon
import com.composables.uikit.components.IntermediateProgressIndicator
import com.composables.uikit.styling.card
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.outline
import com.composeunstyled.outline
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
fun PullToRefreshIndicator(
    isRefreshing: Boolean,
    pullProgress: Float,
    modifier: Modifier = Modifier
) {
    val showIndicator = isRefreshing || pullProgress > 0f

    AnimatedVisibility(
        visible = showIndicator,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(durationMillis = 250)),
        modifier = modifier
    ) {
        val yOffset = if (isRefreshing) 16.dp else (pullProgress * 48).dp

        Box(
            modifier = Modifier
                .offset { IntOffset(0, yOffset.roundToPx()) }
                .shadow(4.dp, CircleShape)
                .background(Theme[colors][card], CircleShape)
                .outline(1.dp, Theme[colors][outline], CircleShape)
                .padding(8.dp)
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRefreshing) {
                IntermediateProgressIndicator(
                    modifier = Modifier.size(20.dp)
                )
            } else {
                val rotation by animateFloatAsState(targetValue = pullProgress * 180f)
                val scale by animateFloatAsState(targetValue = pullProgress.coerceIn(0f, 1f))

                Box(
                    modifier = Modifier
                        .rotate(rotation)
                        .scale(scale)
                ) {
                    Icon(
                        imageVector = Lucide.ArrowDown,
                        contentDescription = stringResource(Res.string.accessibility_pull_to_refresh),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}