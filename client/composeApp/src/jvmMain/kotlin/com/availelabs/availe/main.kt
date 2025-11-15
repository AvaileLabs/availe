package com.availelabs.availe

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.di.createApplicationContainer
import com.availelabs.availe.features.root.RootComponent
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect

@OptIn(ExperimentalOpenIdConnect::class)
fun main() = application {
    val windowState = rememberWindowState()
    val lifecycle = remember { LifecycleRegistry() }

    LifecycleController(lifecycle, windowState)

    val root = remember {
        val activity = PlatformActivity()
        val context = DefaultComponentContext(lifecycle = lifecycle)

        val appContainer = createApplicationContainer(lifecycle, activity)

        RootComponent(
            componentContext = context,
            appContainer = appContainer
        )
    }

    Window(
        onCloseRequest = {
            exitApplication()
        },
        title = "availe",
        state = windowState,
    ) {
        App(root = root)
    }
}