package com.availelabs.availe.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackEvent.SwipeEdge

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
    content()
}