package com.availelabs.availe.data.network

actual fun Throwable.isPlatformSpecificNetworkError(): Boolean {
    return true
}