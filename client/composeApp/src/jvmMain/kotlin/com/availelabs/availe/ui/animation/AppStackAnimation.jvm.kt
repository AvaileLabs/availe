package com.availelabs.availe.ui.animation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.*
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.essenty.backhandler.BackHandler

@OptIn(ExperimentalDecomposeApi::class)
@Composable
actual fun <C : Any, T : Any> appStackAnimation(
    backHandler: BackHandler,
    onBack: () -> Unit
): StackAnimation<C, T> {
    return predictiveBackAnimation(
        backHandler = backHandler,
        fallbackAnimation = stackAnimation(fade() + scale()),
        onBack = onBack
    )
}