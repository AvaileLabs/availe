package com.availelabs.availe.data.network

actual fun Throwable.classifyPlatformError(): NetworkErrorType? {
    return NetworkErrorType.Recoverable.Connectivity
}