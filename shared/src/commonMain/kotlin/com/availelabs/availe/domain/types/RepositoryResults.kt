package com.availelabs.availe.domain.types

sealed interface DbInsertResult<out T> {
    data class Inserted<T>(val value: T) : DbInsertResult<T>
    data object AlreadyExists : DbInsertResult<Nothing>
}

sealed interface DbFindResult<out T> {
    data class Found<T>(val value: T) : DbFindResult<T>
    data object NotFound : DbFindResult<Nothing>
}

sealed interface DbUpdateResult<out T> {
    data class Updated<T>(val value: T) : DbUpdateResult<T>
    data object NotFound : DbUpdateResult<Nothing>
}

sealed interface DbDeleteResult {
    data object Deleted : DbDeleteResult
    data object NotFound : DbDeleteResult
}