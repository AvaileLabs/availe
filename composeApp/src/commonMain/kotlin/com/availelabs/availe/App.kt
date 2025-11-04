package com.availelabs.availe

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.availelabs.availe.ui.decompose.IRootComponent
import com.availelabs.availe.ui.screens.ErrorScreen
import com.availelabs.availe.ui.screens.LoadingScreen
import com.availelabs.availe.ui.screens.LoginScreen
import com.availelabs.availe.ui.screens.MainScreen

@Composable
fun App(root: IRootComponent) {
    Children(
        stack = root.childStack,
    ) {
        when (val child = it.instance) {
            is IRootComponent.Child.Loading -> LoadingScreen(child.component)
            is IRootComponent.Child.Login -> LoginScreen(child.component)
            is IRootComponent.Child.Main -> MainScreen(child.component)
            is IRootComponent.Child.Error -> ErrorScreen(child.component)
        }
    }
}