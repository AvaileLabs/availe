package com.availelabs.availe.domain.types

interface InsertResult {
    data class Inserted<T>(val value: T) : InsertResult
    data object AlreadyExists : InsertResult
}