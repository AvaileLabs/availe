package com.availelabs.availe.model

import com.availelabs.availe.model.error.AppError
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@JvmInline
value class ToastId(val value: String)

sealed interface ToastMessage {
    val id: ToastId
    val durationMilliseconds: Long

    @OptIn(ExperimentalUuidApi::class)
    data class Success(
        val message: String,
        override val id: ToastId = ToastId(Uuid.random().toString()),
        override val durationMilliseconds: Long = 4000L
    ) : ToastMessage

    @OptIn(ExperimentalUuidApi::class)
    data class Error(
        val error: AppError,
        override val id: ToastId = ToastId(Uuid.random().toString()),
        override val durationMilliseconds: Long = 4000L
    ) : ToastMessage

    @OptIn(ExperimentalUuidApi::class)
    data class Info(
        val message: String,
        override val id: ToastId = ToastId(Uuid.random().toString()),
        override val durationMilliseconds: Long = 4000L
    ) : ToastMessage
}