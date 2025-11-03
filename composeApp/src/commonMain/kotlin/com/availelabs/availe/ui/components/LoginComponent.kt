package com.availelabs.availe.ui.components

import com.arkivanov.decompose.ComponentContext

interface ILoginComponent {
    fun onLoginClicked()
}

class LoginComponent(
    componentContext: ComponentContext,
    private val onLogin: () -> Unit
) : ILoginComponent, ComponentContext by componentContext {

    override fun onLoginClicked() {
        onLogin()
    }
}