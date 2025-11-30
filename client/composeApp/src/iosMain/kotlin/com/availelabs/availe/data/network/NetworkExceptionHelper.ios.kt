package com.availelabs.availe.data.network

import io.ktor.client.engine.darwin.*
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut

actual fun Throwable.classifyPlatformError(): NetworkErrorType? {
    if (this is DarwinHttpRequestException) {
        val error = this.origin
        if (error.domain == NSURLErrorDomain) {
            return when (error.code) {
                NSURLErrorNotConnectedToInternet -> NetworkErrorType.Recoverable.Connectivity
                NSURLErrorCannotConnectToHost -> NetworkErrorType.Recoverable.ServerSide
                NSURLErrorTimedOut -> NetworkErrorType.Recoverable.ConnectionTimeout
                else -> NetworkErrorType.Recoverable.Connectivity
            }
        }
        return NetworkErrorType.Recoverable.Connectivity
    }
    return null
}