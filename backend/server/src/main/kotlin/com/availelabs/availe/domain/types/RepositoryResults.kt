package com.availelabs.availe.domain.types

internal sealed interface DbInsertResult<out T> {
    data class Inserted<T>(val value: T) : DbInsertResult<T>
    data object AlreadyExists : DbInsertResult<Nothing>
}

internal sealed interface DbFindResult<out T> {
    data class Found<T>(val value: T) : DbFindResult<T>
    data object NotFound : DbFindResult<Nothing>
}

internal sealed interface DbUpdateResult<out T> {
    data class Updated<T>(val value: T) : DbUpdateResult<T>
    data object NotFound : DbUpdateResult<Nothing>
}

internal sealed interface DbDeleteResult {
    data object Deleted : DbDeleteResult
    data object NotFound : DbDeleteResult
}