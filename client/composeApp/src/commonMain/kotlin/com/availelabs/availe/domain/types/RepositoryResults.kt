package com.availelabs.availe.domain.types

import com.availelabs.availe.domain.model.UserFriendlyErrorMessage

sealed interface RepositoryOperationResult<out T> {
    data class Success<T>(val value: T) : RepositoryOperationResult<T>
    data class Failure(val error: UserFriendlyErrorMessage) : RepositoryOperationResult<Nothing>
}