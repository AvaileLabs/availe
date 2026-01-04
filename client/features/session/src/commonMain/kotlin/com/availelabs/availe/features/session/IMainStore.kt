package com.availelabs.availe.features.session

import com.arkivanov.mvikotlin.core.store.Store
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.ui.model.UiText

interface IMainStore : Store<IMainStore.Intent, IMainStore.State, IMainStore.Label> {

    sealed interface Intent {
        data object LoginRequested : Intent
        data object LogoutRequested : Intent
        data object CancelLoginRequested : Intent
        data object ClearErrorRequested : Intent
    }

    data class State(
        val isAuthenticated: Boolean = false,
        val isAuthenticationServiceAvailable: Boolean = true,
        val isLoading: Boolean = false,
        val errorMessage: UiText? = null
    )

    sealed interface Label {
        data class ErrorOccurred(val error: AppError) : Label
        data class InfoOccurred(val message: String) : Label
    }
}