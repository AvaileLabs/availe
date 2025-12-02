package com.availelabs.availe.data.network

import kotlinx.browser.window

actual fun Throwable.classifyPlatformError(): NetworkErrorType? {
    val isInternetOffline = !window.navigator.onLine

    return if (isInternetOffline) {
        NetworkErrorType.Recoverable.Connectivity
    } else {
        NetworkErrorType.Recoverable.ServerSide
    }
}