package com.availelabs.availe.services

import com.availelabs.availe.model.ToastId
import com.availelabs.availe.model.ToastMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MessageService(
    private val scope: CoroutineScope
) : IMessageService {

    private val _toastMessages = MutableStateFlow<List<ToastMessage>>(emptyList())
    override val toastMessages: StateFlow<List<ToastMessage>> = _toastMessages.asStateFlow()

    override fun show(message: ToastMessage) {
        _toastMessages.update { currentList -> currentList + message }

        scope.launch {
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