package com.availelabs.availe.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackEvent.SwipeEdge

@ExperimentalDecomposeApi
@Composable
expect fun PredictiveBackGestureOverlay(
    backDispatcher: BackDispatcher?,
    backIcon: (@Composable (progress: Float, edge: SwipeEdge) -> Unit)?,
    modifier: Modifier = Modifier,
    startEdgeEnabled: Boolean = true,
    endEdgeEnabled: Boolean = true,
    edgeWidth: Dp = 16.dp,
    activationOffsetThreshold: Dp = 16.dp,
    confirmationProgressThreshold: Float = 0.2F,
    onClose: (() -> Unit)? = null,
    content: @Composable () -> Unit,
)