package com.availelabs.availe.data.network

import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.util.network.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.SerializationException

sealed interface NetworkErrorType {
    val isSafeToPreserveLocalData: Boolean

    sealed interface Recoverable : NetworkErrorType {
        data object ConnectionTimeout : Recoverable {
            override val isSafeToPreserveLocalData: Boolean = true
        }

        data object Connectivity : Recoverable {
            override val isSafeToPreserveLocalData: Boolean = true
        }

        data object ServerSide : Recoverable {
            override val isSafeToPreserveLocalData: Boolean = true
        }

        data object ClientSide : Recoverable {
            override val isSafeToPreserveLocalData: Boolean = false
        }

        data object DataSerialization : Recoverable {
            override val isSafeToPreserveLocalData: Boolean = false
        }
    }

    data object Unrecoverable : NetworkErrorType {
        override val isSafeToPreserveLocalData: Boolean = false
    }
}

fun Throwable.classifyNetworkError(): NetworkErrorType {
    var currentThrowable: Throwable? = this
    while (currentThrowable != null) {
        val networkErrorType = currentThrowable.identifySpecificNetworkError()
        if (networkErrorType != null) {
            return networkErrorType
        }
        currentThrowable = currentThrowable.cause
    }
    return NetworkErrorType.Unrecoverable
}

private fun Throwable.identifySpecificNetworkError(): NetworkErrorType? {
    return when (this) {
        is HttpRequestTimeoutException -> NetworkErrorType.Recoverable.ConnectionTimeout
        is ConnectTimeoutException -> NetworkErrorType.Recoverable.ConnectionTimeout
        is SocketTimeoutException -> NetworkErrorType.Recoverable.ConnectionTimeout
        is TimeoutCancellationException -> NetworkErrorType.Recoverable.ConnectionTimeout
        is UnresolvedAddressException -> NetworkErrorType.Recoverable.Connectivity
        is ClientRequestException -> NetworkErrorType.Recoverable.ClientSide
        is ServerResponseException -> NetworkErrorType.Recoverable.ServerSide
        is SerializationException -> NetworkErrorType.Recoverable.DataSerialization
        else -> classifyPlatformError()
    }
}

expect fun Throwable.classifyPlatformError(): NetworkErrorType?