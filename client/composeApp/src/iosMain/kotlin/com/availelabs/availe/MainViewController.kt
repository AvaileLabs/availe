package com.availelabs.availe

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.availelabs.availe.root.IRootComponent
import platform.UIKit.UIViewController

fun MainViewController(root: IRootComponent): UIViewController {
    return ComposeUIViewController(
        configure = {
            onFocusBehavior = OnFocusBehavior.DoNothing
        }
    ) {
        App(
            rootComponent = root
        )
    }
}