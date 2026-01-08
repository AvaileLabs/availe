package com.availelabs.availe.platform

import kotlinx.browser.window

actual object UrlHandler {
    actual fun openInNewTab(url: String) {
        window.open(url, "_blank")
    }
}