package com.availelabs.availe.core.platform

import kotlinx.browser.window

class JsInAppBrowserLauncher : IInAppBrowserLauncher {
    override fun open(url: String) {
        window.open(url, "_blank")
    }
}

actual fun createInAppBrowserLauncher(activity: PlatformActivity): IInAppBrowserLauncher {
    return JsInAppBrowserLauncher()
}