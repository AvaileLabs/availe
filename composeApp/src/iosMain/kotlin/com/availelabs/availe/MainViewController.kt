package com.availelabs.availe

import androidx.compose.ui.window.ComposeUIViewController

internal fun MainViewController(root: IRootComponent) = ComposeUIViewController { App(root = root) }