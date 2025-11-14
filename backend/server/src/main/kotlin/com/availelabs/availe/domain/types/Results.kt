package com.availelabs.availe.domain.types

internal sealed interface InsertResult<out T> {
    data class Inserted<T>(val value: T) : InsertResult<T>
    data object AlreadyExists : InsertResult<Nothing>
}

internal sealed interface FindResult<out T> {
    data class Found<T>(val value: T) : FindResult<T>
    data object NotFound : FindResult<Nothing>
}

internal sealed interface UpdateResult<out T> {
    data class Updated<T>(val value: T) : UpdateResult<T>
    data object NotFound : UpdateResult<Nothing>
    data object Forbidden : UpdateResult<Nothing>
}

internal sealed interface DeleteResult {
    data object Deleted : DeleteResult
    data object NotFound : DeleteResult
    data object Forbidden : DeleteResult
}