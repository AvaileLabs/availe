package com.availelabs.availe.network

import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

actual fun Throwable.classifyPlatformError(): NetworkErrorType? {
    return when (this) {
        is UnknownHostException -> NetworkErrorType.Recoverable.Connectivity
        is ConnectException -> NetworkErrorType.Recoverable.ServerSide
        is SocketTimeoutException -> NetworkErrorType.Recoverable.ConnectionTimeout
        is SocketException -> {
            if (message == "Socket closed") {
                null
            } else {
                NetworkErrorType.Recoverable.Connectivity
            }
        }

        is SSLException -> NetworkErrorType.Recoverable.Connectivity
        else -> null
    }
}