package com.availelabs.availe

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.data.auth.handleRedirect
import com.availelabs.availe.di.createApplicationContainer
import com.availelabs.availe.features.root.RootComponent
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import web.dom.DocumentVisibilityState
import web.dom.document
import web.dom.visible
import web.events.Event
import web.events.VISIBILITY_CHANGE
import web.events.addEventListener

@OptIn(ExperimentalComposeUiApi::class, ExperimentalOpenIdConnect::class)
fun main() {
    if (handleRedirect()) {
        return
    }

    val lifecycle = LifecycleRegistry()
    val activity = PlatformActivity()
    val context = DefaultComponentContext(lifecycle = lifecycle)

    val appContainer = createApplicationContainer(lifecycle, activity)

    val root = RootComponent(
        componentContext = context,
        appContainer = appContainer
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