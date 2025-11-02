package com.availelabs.availe.ui.components

import com.arkivanov.decompose.ComponentContext

interface LoginComponent {
    fun onLoginClicked()
}

class DefaultLoginComponent(
    componentContext: ComponentContext,
    private val onLogin: () -> Unit
) : LoginComponent, ComponentContext by componentContext {

    override fun onLoginClicked() {
        onLogin()
    }
}