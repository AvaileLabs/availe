package com.availelabs.availe.data.network

import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

actual fun Throwable.isPlatformSpecificNetworkError(): Boolean {
    return when (this) {
        is UnknownHostException -> true
        is SocketException -> true
        is SSLException -> true
        else -> false
    }
}