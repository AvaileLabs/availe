package com.availelabs.availe.ui.animation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.essenty.backhandler.BackHandler

@Composable
expect fun <C : Any, T : Any> appStackAnimation(
    backHandler: BackHandler,
    onBack: () -> Unit
): StackAnimation<C, T>