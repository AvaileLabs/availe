package com.availelabs.availe.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackEvent.SwipeEdge
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureOverlay as DecomposePredictiveBackGestureOverlay

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
        DecomposePredictiveBackGestureOverlay(
            backDispatcher = backDispatcher,
            backIcon = backIcon,
            modifier = modifier,
            startEdgeEnabled = startEdgeEnabled,
            endEdgeEnabled = endEdgeEnabled,
            edgeWidth = 48.dp,
            activationOffsetThreshold = activationOffsetThreshold,
            confirmationProgressThreshold = confirmationProgressThreshold,
            onClose = onClose,
            content = content
        )
    } else {
        content()
    }
}