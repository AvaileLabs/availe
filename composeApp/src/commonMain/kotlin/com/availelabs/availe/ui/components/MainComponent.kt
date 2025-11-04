package com.availelabs.availe.ui.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value

sealed interface MainUiState {
    data object Idle : MainUiState
    data object LoggingOut : MainUiState
}

interface IMainComponent {
    val uiState: Value<MainUiState>
    fun onLogoutClicked()
}

class MainComponent(
    componentContext: ComponentContext,
    override val uiState: Value<MainUiState>,
    private val onLogout: () -> Unit
) : IMainComponent, ComponentContext by componentContext {

    override fun onLogoutClicked() {
        onLogout()
    }
}