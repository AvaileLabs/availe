package com.availelabs.availe.services

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.coroutines.withLifecycle
import com.availelabs.availe.model.ToastId
import com.availelabs.availe.model.ToastMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class MessageService(
    lifecycle: Lifecycle
) : IMessageService {

    private val _toastMessages = MutableStateFlow<List<ToastMessage>>(emptyList())
    override val toastMessages: StateFlow<List<ToastMessage>> = _toastMessages.asStateFlow()

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