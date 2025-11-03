package com.availelabs.availe

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.availelabs.availe.auth.MainScreen
import com.availelabs.availe.ui.components.IRootComponent
import com.availelabs.availe.ui.screen.LoginScreen

@Composable
fun App(root: IRootComponent) {
    Children(
        stack = root.childStack,
    ) {
        when (val child = it.instance) {
            is IRootComponent.Child.Login -> LoginScreen(child.component)
            is IRootComponent.Child.Main -> MainScreen(child.component)
        }
    }
}