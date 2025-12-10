package com.availelabs.availe.domain.model

import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@JvmInline
value class ToastId(val value: String)

sealed interface ToastMessage {
    val id: ToastId
    val text: DisplayMessage
    val durationMilliseconds: Long

    @OptIn(ExperimentalUuidApi::class)
    data class Success(
        override val text: DisplayMessage,
        override val id: ToastId = ToastId(Uuid.random().toString()),
        override val durationMilliseconds: Long = 4000L
    ) : ToastMessage

    @OptIn(ExperimentalUuidApi::class)
    data class Error(
        override val text: DisplayMessage,
        override val id: ToastId = ToastId(Uuid.random().toString()),
        override val durationMilliseconds: Long = 4000L
    ) : ToastMessage

    @OptIn(ExperimentalUuidApi::class)
    data class Info(
        override val text: DisplayMessage,
        override val id: ToastId = ToastId(Uuid.random().toString()),
        override val durationMilliseconds: Long = 4000L
    ) : ToastMessage
}