package com.availelabs.availe.network

import com.availelabs.availe.model.error.AppError

fun Throwable.toAppError(): AppError {
    val networkErrorType = this.classifyNetworkError()

    return when (networkErrorType) {
        is NetworkErrorType.Recoverable.Connectivity -> AppError.NetworkError.NoInternetConnection
        is NetworkErrorType.Recoverable.ConnectionTimeout -> AppError.NetworkError.RequestTimeout
        is NetworkErrorType.Recoverable.ServerSide -> AppError.NetworkError.ServerUnreachable
        is NetworkErrorType.Recoverable.DataSerialization -> AppError.NetworkError.SerializationError
        is NetworkErrorType.Recoverable.ClientSide -> AppError.UnknownError(this.message)
        is NetworkErrorType.Unrecoverable -> AppError.UnknownError(this.message)
    }
}