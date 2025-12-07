package com.availelabs.availe.data.network

import com.availelabs.availe.domain.model.UserFriendlyErrorMessage
import com.availelabs.availe.domain.types.RepositoryOperationResult
import kotlinx.coroutines.CancellationException

suspend fun <T> executeSafeNetworkRequest(
    onFatalError: suspend () -> Unit = {},
    operation: suspend () -> T
): RepositoryOperationResult<T> {
    return try {
        RepositoryOperationResult.Success(operation())
    } catch (cancellationException: CancellationException) {
        throw cancellationException
    } catch (throwable: Throwable) {
        val networkErrorType = throwable.classifyNetworkError()

        if (!networkErrorType.isSafeToPreserveLocalData) {
            onFatalError()
        }

        if (networkErrorType is NetworkErrorType.Unrecoverable) {
            throw throwable
        }

        val errorMessageString = when (networkErrorType) {
            is NetworkErrorType.Recoverable.Connectivity -> "No internet connection. Please check your network."
            is NetworkErrorType.Recoverable.ConnectionTimeout -> "The connection timed out. Please try again."
            is NetworkErrorType.Recoverable.ServerSide -> "The server is experiencing issues. Please try again later."
            is NetworkErrorType.Recoverable.ClientSide -> "Unable to load this data. It may have been removed."
            is NetworkErrorType.Recoverable.DataSerialization -> "Data error. Please ensure you have the latest version of the app."
            is NetworkErrorType.Unrecoverable -> throw throwable
        }

        RepositoryOperationResult.Failure(UserFriendlyErrorMessage(errorMessageString))
    }
}