package com.availelabs.availe.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackEvent.SwipeEdge
import com.availelabs.availe.ui.animation.PredictiveBackGestureOverlayWithOptionForCalculationThreshold
import com.availelabs.availe.ui.configurations.BackGestureConfig

@ExperimentalDecomposeApi
@Composable
actual fun PredictiveBackGestureOverlay(
    backDispatcher: BackDispatcher?,
    backIcon: (@Composable (progress: Float, edge: SwipeEdge) -> Unit)?,
    modifier: Modifier,
    startEdgeEnabled: Boolean,
    endEdgeEnabled: Boolean,
    edgeWidth: Dp,
    activationOffsetThreshold: Dp,
    confirmationProgressThreshold: Float,
    onClose: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    if (backDispatcher != null) {
        PredictiveBackGestureOverlayWithOptionForCalculationThreshold(
            backDispatcher = backDispatcher,
            backIcon = backIcon,
            modifier = modifier,
            startEdgeEnabled = true,
            endEdgeEnabled = false,
            edgeWidth = BackGestureConfig.edgeWidth,
            activationOffsetThreshold = activationOffsetThreshold,
            confirmationProgressThreshold = confirmationProgressThreshold,
            onClose = onClose,
            content = content
        )
    } else {
        content()
    }
}