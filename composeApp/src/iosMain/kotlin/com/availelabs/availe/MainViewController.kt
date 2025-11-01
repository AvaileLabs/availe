package com.availelabs.availe

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.availelabs.availe.auth.PlatformActivity
import com.availelabs.availe.auth.createPlatformAuthFlowFactory
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val lifecycle = LifecycleRegistry()
    val factory = createPlatformAuthFlowFactory(PlatformActivity())

    val root = RootComponent(
        componentContext = DefaultComponentContext(lifecycle = lifecycle),
        codeAuthFlowFactory = factory
    )

    return ComposeUIViewController { App(root = root) }
}