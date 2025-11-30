package com.availelabs.availe.data.network

actual fun Throwable.isRecoverableNetworkError(): Boolean {
    return true
}