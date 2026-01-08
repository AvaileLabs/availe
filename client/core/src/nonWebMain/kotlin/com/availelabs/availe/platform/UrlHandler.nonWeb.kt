package com.availelabs.availe.platform

actual object UrlHandler {
    actual fun openInNewTab(url: String) {
        // No-op on non-web platforms as this concept does not apply
    }
}