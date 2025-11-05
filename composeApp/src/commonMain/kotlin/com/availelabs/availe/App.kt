package com.availelabs.availe

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.availelabs.availe.ui.navigation.Child
import com.availelabs.availe.ui.navigation.IRootComponent
import com.availelabs.availe.ui.screens.home.HomeScreen
import com.composables.uikit.styling.UiKitTheme

@Composable
fun App(root: IRootComponent) {
    UiKitTheme {
        Children(
            stack = root.childStack,
        ) {
            when (val child = it.instance) {
                is Child.Home -> HomeScreen(homeScreenComponent = child.component)
            }
        }
    }
}