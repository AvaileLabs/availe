package com.availelabs.availe.data.services

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.coroutines.withLifecycle
import com.availelabs.availe.domain.model.ToastId
import com.availelabs.availe.domain.model.ToastMessage
import kotlinx.coroutines.*

interface IMessageService {
    val toastMessages: Value<List<ToastMessage>>
    fun show(message: ToastMessage)
    fun dismiss(id: ToastId)
}

class MessageService(
    lifecycle: Lifecycle
) : IMessageService {

    private val _toastMessages = MutableValue<List<ToastMessage>>(emptyList())
    override val toastMessages: Value<List<ToastMessage>> = _toastMessages

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        .withLifecycle(lifecycle)

    override fun show(message: ToastMessage) {
        _toastMessages.update { currentList -> currentList + message }

        coroutineScope.launch {
            delay(message.durationMilliseconds)
            dismiss(message.id)
        }
    }

    override fun dismiss(id: ToastId) {
        _toastMessages.update { currentList ->
            currentList.filter { it.id != id }
        }
    }
}