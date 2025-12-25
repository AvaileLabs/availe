package com.availelabs.availe.ui.mapper

import availe.client.composeapp.generated.resources.*
import com.availelabs.availe.auth.LoginError
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.ui.model.UiText

object ErrorMessageMapper {
    fun map(error: AppError): UiText {
        return when (error) {
            is AppError.NetworkError.NoInternetConnection -> UiText.ResourceString(Res.string.error_no_internet)
            is AppError.NetworkError.RequestTimeout -> UiText.ResourceString(Res.string.error_request_timeout)
            is AppError.NetworkError.ServerUnreachable -> UiText.ResourceString(Res.string.error_server_unreachable)
            is AppError.NetworkError.SerializationError -> UiText.ResourceString(Res.string.error_serialization)

            is AppError.AuthenticationError.SessionExpired -> UiText.ResourceString(Res.string.error_session_expired)
            is AppError.AuthenticationError.InvalidCredentials -> UiText.ResourceString(Res.string.error_invalid_credentials)

            is AppError.BusinessError.ResourceNotFound -> UiText.ResourceString(Res.string.error_resource_not_found)
            is AppError.BusinessError.OperationForbidden -> UiText.ResourceString(Res.string.error_operation_forbidden)

            is AppError.UnknownError -> UiText.ResourceString(Res.string.error_unknown)
        }
    }

    fun map(error: LoginError): UiText {
        return when (error) {
            LoginError.NoInternetConnection -> UiText.ResourceString(Res.string.error_no_internet)
            LoginError.ServerUnreachable -> UiText.ResourceString(Res.string.error_server_unreachable)
            LoginError.InvalidCredentials -> UiText.ResourceString(Res.string.error_invalid_credentials)
            is LoginError.Unexpected -> UiText.VerbatimString(error.debugMessage)
        }
    }
}