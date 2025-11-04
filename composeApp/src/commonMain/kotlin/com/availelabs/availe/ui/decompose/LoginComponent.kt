package com.availelabs.availe.ui.decompose

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
}

interface ILoginComponent {
    val uiState: Value<LoginUiState>
    fun onLoginClicked()
}

class LoginComponent(
    componentContext: ComponentContext,
    override val uiState: Value<LoginUiState>,
    private val onLogin: () -> Unit
) : ILoginComponent, ComponentContext by componentContext {

    override fun onLoginClicked() {
        onLogin()
    }
}