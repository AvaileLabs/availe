package com.availelabs.availe.data.extensions

import com.availelabs.availe.data.network.toAppError
import com.availelabs.availe.domain.model.error.AppError
import org.mobilenativefoundation.store.store5.StoreReadResponse

fun StoreReadResponse.Error.toAppError(): AppError {
    return when (this) {
        is StoreReadResponse.Error.Exception -> this.error.toAppError()
        is StoreReadResponse.Error.Message -> AppError.UnknownError(this.message)
        is StoreReadResponse.Error.Custom<*> -> {
            if (this.error is Throwable) {
                (this.error as Throwable).toAppError()
            } else {
                AppError.UnknownError(this.error.toString())
            }
        }
    }
}