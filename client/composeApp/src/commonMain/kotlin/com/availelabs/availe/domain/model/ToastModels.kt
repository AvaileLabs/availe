package com.availelabs.availe.domain.model

import com.availelabs.availe.ui.model.UiText
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@JvmInline
value class ToastId(val value: String)

sealed interface ToastMessage {
    val id: ToastId
    val text: UiText
    val durationMilliseconds: Long

    @OptIn(ExperimentalUuidApi::class)
    data class Success(
        override val text: UiText,
        override val id: ToastId = ToastId(Uuid.random().toString()),
        override val durationMilliseconds: Long = 4000L
    ) : ToastMessage

    @OptIn(ExperimentalUuidApi::class)
    data class Error(
        override val text: UiText,
        override val id: ToastId = ToastId(Uuid.random().toString()),
        override val durationMilliseconds: Long = 4000L
    ) : ToastMessage

    @OptIn(ExperimentalUuidApi::class)
    data class Info(
        override val text: UiText,
        override val id: ToastId = ToastId(Uuid.random().toString()),
        override val durationMilliseconds: Long = 4000L
    ) : ToastMessage
}