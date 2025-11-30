package com.availelabs.availe.data.network

import com.availelabs.availe.domain.model.UserFriendlyErrorMessage
import com.availelabs.availe.domain.types.RepositoryOperationResult
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.util.network.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

suspend fun <T> executeSafeNetworkRequest(
    operation: suspend () -> T
): RepositoryOperationResult<T> {
    return try {
        RepositoryOperationResult.Success(operation())
    } catch (cancellationException: CancellationException) {
        throw cancellationException
    } catch (throwable: Throwable) {
        if (!throwable.isRecoverableNetworkError()) {
            throw throwable
        }

        val errorMessage = when (throwable) {
            is UnresolvedAddressException -> "No internet connection. Please check your network."
            is HttpRequestTimeoutException -> "The request timed out. Please try again."
            is ConnectTimeoutException -> "The server is taking too long to respond."
            is SocketTimeoutException -> "The connection was interrupted. Please try again."
            is ClientRequestException -> "Unable to load this data. It may have been removed."
            is ServerResponseException -> "The server is experiencing issues. Please try again later."
            is SerializationException -> "Data error. Please ensure you have the latest version of the app."
            else -> "Unable to connect to the server."
        }
        RepositoryOperationResult.Failure(UserFriendlyErrorMessage(errorMessage))
    }
}