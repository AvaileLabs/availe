package com.availelabs.availe.login

import com.arkivanov.decompose.ComponentContext

interface ILoginComponent {
    fun onDismissClicked()
}

class LoginComponent(
    componentContext: ComponentContext,
    private val navigator: ILoginNavigator
) : ILoginComponent, ComponentContext by componentContext {

    override fun onDismissClicked() {
        navigator.dismissLogin()
    }
}