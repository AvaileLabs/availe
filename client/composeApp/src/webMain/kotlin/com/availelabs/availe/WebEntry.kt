package com.availelabs.availe

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import com.arkivanov.essenty.statekeeper.StateKeeper
import com.availelabs.availe.di.createApplicationContainer
import com.availelabs.availe.platform.PlatformActivity
import com.availelabs.availe.root.RootComponent
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.url.URLSearchParams
import web.dom.DocumentVisibilityState
import web.dom.document
import web.dom.visible
import web.events.Event
import web.events.VISIBILITY_CHANGE
import web.events.addEventListener
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalWasmJsInterop::class,
    ExperimentalDecomposeApi::class
)
fun startWebApp(deepLink: String?, stateKeeper: StateKeeper): RootComponent {
    val lifecycle = LifecycleRegistry()
    val activity = PlatformActivity()

    val context = DefaultComponentContext(
        lifecycle = lifecycle,
        stateKeeper = stateKeeper
    )

    val appContainer = createApplicationContainer(lifecycle, activity)

    val searchParams = URLSearchParams(window.location.search.toJsString())
    val code = searchParams.get("code")

    if (code != null) {
        MainScope().launch {
            appContainer.authenticationRepository.resumeLogin(code, appContainer.sessionManager::updateState)
        }
    }

    val root = RootComponent(
        componentContext = context,
        appContainer = appContainer,
        deepLinkUrl = deepLink
    )

    lifecycle.attachToDocument()

    ComposeViewport {
        App(
            rootComponent = root
        )
    }

    return root
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