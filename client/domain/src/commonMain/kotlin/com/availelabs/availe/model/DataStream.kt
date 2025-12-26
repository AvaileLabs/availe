package com.availelabs.availe.model

import com.availelabs.availe.model.error.AppError

sealed interface DataStream<out T> {
    data object Loading : DataStream<Nothing>
    data class Data<T>(val value: T) : DataStream<T>
    data class Error(val error: AppError) : DataStream<Nothing>
}