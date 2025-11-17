package com.availelabs.availe.features.profile

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.availelabs.availe.data.auth.IAuthenticationService
import com.availelabs.availe.domain.auth.AuthenticationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface IProfileComponent {
    val uiState: Value<ProfileState>
    fun onEvent(event: ProfileEvents)
}

class ProfileComponent(
    componentContext: ComponentContext,
    private val authService: IAuthenticationService,
    private val onOpenUrl: (String) -> Unit
) : IProfileComponent, ComponentContext by componentContext {

    private val scope = componentContext.coroutineScope(Dispatchers.Main.immediate)

    private val _uiState = MutableValue(ProfileState())
    override val uiState: Value<ProfileState> = _uiState

    init {
        authService.authenticationState.subscribe { authState ->
            scope.launch {
                updateProfileState(authState)
            }
        }
    }

    private suspend fun updateProfileState(authState: AuthenticationState) {
        when (authState) {
            is AuthenticationState.LoggedIn -> {
                val profile = authService.getCurrentUserProfile()
                _uiState.update {
                    it.copy(
                        isAuthenticated = true,
                        userProfile = profile,
                        isLoading = false
                    )
                }
            }

            is AuthenticationState.LoggedOut, AuthenticationState.Unknown, is AuthenticationState.InitializationFailed -> {
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        userProfile = null,
                        isLoading = false
                    )
                }
            }
        }
    }

    override fun onEvent(event: ProfileEvents) {
        when (event) {
            ProfileEvents.LoginClicked -> {
                _uiState.update { it.copy(isLoading = true) }
                scope.launch {
                    authService.login()
                }
            }

            ProfileEvents.LogoutClicked -> {
                _uiState.update { it.copy(isLoading = true) }
                scope.launch {
                    authService.logout()
                }
            }

            ProfileEvents.ManageAccountClicked -> {
                val url = authService.getAccountManagementUrl()
                onOpenUrl(url)
            }
        }
    }
}