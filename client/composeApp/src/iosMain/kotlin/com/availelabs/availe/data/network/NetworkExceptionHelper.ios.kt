package com.availelabs.availe.data.network

import io.ktor.client.engine.darwin.*
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.TimeoutCancellationException

actual fun Throwable.isRecoverableNetworkError(): Boolean {
    return when (this) {
        is DarwinHttpRequestException -> true
        is HttpRequestTimeoutException -> true
        is ConnectTimeoutException -> true
        is SocketTimeoutException -> true
        is TimeoutCancellationException -> true
        is ServerResponseException -> true
        is ClientRequestException -> true
        else -> false
    }
}