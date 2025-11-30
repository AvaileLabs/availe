package com.availelabs.availe.data.network

import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.util.network.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.SerializationException

sealed interface NetworkErrorType {
    sealed interface Recoverable : NetworkErrorType {
        data object ConnectionTimeout : Recoverable
        data object Connectivity : Recoverable
        data object ServerSide : Recoverable
        data object ClientSide : Recoverable
        data object DataSerialization : Recoverable
    }

    data object Unrecoverable : NetworkErrorType
}

fun Throwable.classifyNetworkError(): NetworkErrorType {
    return when (this) {
        is HttpRequestTimeoutException -> NetworkErrorType.Recoverable.ConnectionTimeout
        is ConnectTimeoutException -> NetworkErrorType.Recoverable.ConnectionTimeout
        is SocketTimeoutException -> NetworkErrorType.Recoverable.ConnectionTimeout
        is TimeoutCancellationException -> NetworkErrorType.Recoverable.ConnectionTimeout

        is UnresolvedAddressException -> NetworkErrorType.Recoverable.Connectivity

        is ClientRequestException -> NetworkErrorType.Recoverable.ClientSide
        is ServerResponseException -> NetworkErrorType.Recoverable.ServerSide
        is SerializationException -> NetworkErrorType.Recoverable.DataSerialization

        else -> if (isPlatformSpecificNetworkError()) {
            NetworkErrorType.Recoverable.Connectivity
        } else {
            NetworkErrorType.Unrecoverable
        }
    }
}

expect fun Throwable.isPlatformSpecificNetworkError(): Boolean