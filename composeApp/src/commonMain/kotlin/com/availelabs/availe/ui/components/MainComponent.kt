package com.availelabs.availe.ui.components

import com.arkivanov.decompose.ComponentContext

interface MainComponent {
    fun onLogoutClicked()
}

class DefaultMainComponent(
    componentContext: ComponentContext,
    private val onLogout: () -> Unit
) : MainComponent, ComponentContext by componentContext {

    override fun onLogoutClicked() {
        onLogout()
    }
}