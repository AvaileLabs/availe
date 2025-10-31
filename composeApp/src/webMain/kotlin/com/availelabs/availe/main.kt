package com.availelabs.availe

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import web.dom.DocumentVisibilityState
import web.dom.document
import web.dom.visible
import web.events.Event
import web.events.VISIBILITY_CHANGE
import web.events.addEventListener

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val lifecycle = LifecycleRegistry()

    val root = RootComponent(
        componentContext = DefaultComponentContext(lifecycle = lifecycle),
    )

    lifecycle.attachToDocument()

    ComposeViewport {
        App(root = root)
    }
}

private fun LifecycleRegistry.attachToDocument() {
    fun onVisibilityChanged() {
        if (document.visibilityState == DocumentVisibilityState.visible) {
            resume()
        } else {
            stop()
        }
    }

    onVisibilityChanged()

    document.addEventListener(
        type = Event.VISIBILITY_CHANGE,
        handler = { _: Event -> onVisibilityChanged() }
    )
}