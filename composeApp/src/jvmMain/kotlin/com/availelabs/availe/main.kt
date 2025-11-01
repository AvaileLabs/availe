package com.availelabs.availe

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.availelabs.availe.auth.PlatformActivity
import com.availelabs.availe.auth.createPlatformAuthFlowFactory

fun main() {
    val lifecycle = LifecycleRegistry()
    val authFactory = createPlatformAuthFlowFactory(PlatformActivity())
    val root = RootComponent(
        componentContext = DefaultComponentContext(lifecycle = lifecycle),
        codeAuthFlowFactory = authFactory
    )

    application {
        val windowState = rememberWindowState()
        LifecycleController(lifecycle, windowState)

        Window(
            onCloseRequest = ::exitApplication,
            title = "availe",
            state = windowState,
        ) {
            App(root = root)
        }
    }
}