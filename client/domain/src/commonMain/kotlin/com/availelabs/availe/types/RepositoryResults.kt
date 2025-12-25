package com.availelabs.availe.types

import com.availelabs.availe.model.error.AppError

sealed interface RepositoryOperationResult<out T> {
    data class Success<T>(val value: T) : RepositoryOperationResult<T>
    data class Failure(val error: AppError) : RepositoryOperationResult<Nothing>
}