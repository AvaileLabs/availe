package com.availelabs.availe.features.login

import com.arkivanov.decompose.ComponentContext

interface ILoginComponent {
    fun onDismissClicked()
}

class LoginComponent(
    componentContext: ComponentContext,
    private val onDismiss: () -> Unit
) : ILoginComponent, ComponentContext by componentContext {

    override fun onDismissClicked() {
        onDismiss()
    }
}