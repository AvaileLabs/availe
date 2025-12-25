package com.availelabs.availe.services

import com.availelabs.availe.model.ToastId
import com.availelabs.availe.model.ToastMessage
import kotlinx.coroutines.flow.StateFlow

interface IMessageService {
    val toastMessages: StateFlow<List<ToastMessage>>
    fun show(message: ToastMessage)
    fun dismiss(id: ToastId)
}