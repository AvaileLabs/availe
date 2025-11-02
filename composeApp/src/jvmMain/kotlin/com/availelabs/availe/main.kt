package com.availelabs.availe

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.availelabs.availe.data.auth.PlatformActivity
import com.availelabs.availe.data.auth.createPlatformAuthFlowFactory
import com.availelabs.availe.data.auth.createTokenStore
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect

@OptIn(ExperimentalOpenIdConnect::class)
fun main() = application {
    val windowState = rememberWindowState()
    val lifecycle = remember { LifecycleRegistry() }
    val activity = remember { PlatformActivity() }
    val authFactory = remember { createPlatformAuthFlowFactory(activity) }
    val tokenStore = remember { createTokenStore(activity) }

    LifecycleController(lifecycle, windowState)

    val root = remember {
        RootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
            codeAuthFlowFactory = authFactory,
            tokenStore = tokenStore
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "availe",
        state = windowState,
    ) {
        App(root = root)
    }
}