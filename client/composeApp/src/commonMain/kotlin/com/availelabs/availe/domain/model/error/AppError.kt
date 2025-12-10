package com.availelabs.availe.domain.model.error

sealed interface AppError {
    sealed interface NetworkError : AppError {
        data object NoInternetConnection : NetworkError
        data object RequestTimeout : NetworkError
        data object ServerUnreachable : NetworkError
        data object SerializationError : NetworkError
    }

    sealed interface AuthenticationError : AppError {
        data object SessionExpired : AuthenticationError
        data object InvalidCredentials : AuthenticationError
    }

    sealed interface BusinessError : AppError {
        data object ResourceNotFound : BusinessError
        data object OperationForbidden : BusinessError
    }

    data class UnknownError(val debugMessage: String?) : AppError
}