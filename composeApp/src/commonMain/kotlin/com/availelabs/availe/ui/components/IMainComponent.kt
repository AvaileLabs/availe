package com.availelabs.availe.ui.components

import com.arkivanov.decompose.ComponentContext

interface IMainComponent {
    fun onLogoutClicked()
}

class MainComponent(
    componentContext: ComponentContext,
    private val onLogout: () -> Unit
) : IMainComponent, ComponentContext by componentContext {

    override fun onLogoutClicked() {
        onLogout()
    }
}