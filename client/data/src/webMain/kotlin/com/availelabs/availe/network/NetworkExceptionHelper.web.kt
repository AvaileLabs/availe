package com.availelabs.availe.network

import kotlinx.browser.window

actual fun Throwable.classifyPlatformError(): NetworkErrorType? {
    val isInternetOffline = !window.navigator.onLine

    return if (isInternetOffline) {
        NetworkErrorType.Recoverable.Connectivity
    } else {
        NetworkErrorType.Recoverable.ServerSide
    }
}