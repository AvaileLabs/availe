package com.availelabs.availe.services

import com.arkivanov.decompose.value.Value
import com.availelabs.availe.model.ToastId
import com.availelabs.availe.model.ToastMessage

interface IMessageService {
    val toastMessages: Value<List<ToastMessage>>
    fun show(message: ToastMessage)
    fun dismiss(id: ToastId)
}