package com.availelabs.availe

import androidx.compose.ui.window.ComposeUIViewController
import com.availelabs.availe.ui.decompose.IRootComponent
import platform.UIKit.UIViewController

fun MainViewController(root: IRootComponent): UIViewController {
    return ComposeUIViewController { App(root = root) }
}