package com.availelabs.availe

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController(root: IRootComponent) = ComposeUIViewController { App(root = root) }