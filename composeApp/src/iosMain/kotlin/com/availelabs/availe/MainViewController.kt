package com.availelabs.availe

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.availelabs.availe.data.auth.PlatformActivity
import com.availelabs.availe.data.auth.createPlatformAuthFlowFactory
import com.availelabs.availe.data.auth.createTokenStore
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import platform.UIKit.UIViewController

@OptIn(ExperimentalOpenIdConnect::class)
fun MainViewController(): UIViewController {
    val lifecycle = LifecycleRegistry()
    val activity = PlatformActivity()
    val factory = createPlatformAuthFlowFactory(activity)
    val tokenStore = createTokenStore(activity)

    val root = RootComponent(
        componentContext = DefaultComponentContext(lifecycle = lifecycle),
        codeAuthFlowFactory = factory,
        tokenStore = tokenStore
    )

    return ComposeUIViewController { App(root = root) }
}