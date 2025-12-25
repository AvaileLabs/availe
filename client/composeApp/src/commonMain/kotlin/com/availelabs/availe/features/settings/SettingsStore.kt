package com.availelabs.availe.features.settings

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.availelabs.availe.domain.model.UserDisplayName
import com.availelabs.availe.repositories.IAuthenticationRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.ui.model.UiText
import kotlinx.coroutines.launch

interface ISettingsStore : Store<ISettingsStore.Intent, ISettingsStore.State, ISettingsStore.Label> {
    sealed interface Intent {
        data object RefreshPage : Intent
        data class UpdateDisplayName(val newName: String) : Intent
        data object EnterKeycloakPage : Intent
        data object Logout : Intent
    }

    data class State(
        val displayName: UserDisplayName = UserDisplayName(""),
        val refreshing: Boolean = false,
        val inlineError: UiText? = null
    )

    sealed interface Label {
        data object SettingsSaved : Label
        data class InfoMessage(val message: UiText) : Label
        data class OpenUrl(val url: String) : Label
    }
}

class SettingsStoreFactory(
    private val storeFactory: StoreFactory,
    private val sessionManager: ISessionRepository,
    private val authenticationRepository: IAuthenticationRepository
) {
    private sealed interface Msg {
        data class SetDisplayName(val name: UserDisplayName) : Msg
        data class SetRefreshing(val isRefreshing: Boolean) : Msg
        data class SetError(val error: UiText?) : Msg
    }

    fun create(): ISettingsStore =
        object : ISettingsStore,
            Store<ISettingsStore.Intent, ISettingsStore.State, ISettingsStore.Label> by storeFactory.create(
                name = "SettingsStore",
                initialState = ISettingsStore.State(),
                bootstrapper = null,
                executorFactory = { SettingsExecutor() },
                reducer = SettingsReducer
            ) {}

    private inner class SettingsExecutor :
        CoroutineExecutor<ISettingsStore.Intent, Nothing, ISettingsStore.State, Msg, ISettingsStore.Label>() {

        override fun executeIntent(intent: ISettingsStore.Intent) {
            when (intent) {
                is ISettingsStore.Intent.RefreshPage -> refreshPage()
                is ISettingsStore.Intent.UpdateDisplayName -> updateDisplayName(intent.newName)
                is ISettingsStore.Intent.Logout -> logout()
                is ISettingsStore.Intent.EnterKeycloakPage -> openKeycloakPage()
            }
        }

        private fun openKeycloakPage() {
            val url = sessionManager.getAccountManagementUrl()
            publish(ISettingsStore.Label.OpenUrl(url))
        }

        private fun refreshPage() {
            dispatch(Msg.SetRefreshing(true))
            dispatch(Msg.SetError(null))
            scope.launch {
                try {
                    val profile = sessionManager.getCurrentUserProfile()
                    if (profile != null) {
                        dispatch(
                            Msg.SetDisplayName(
                                UserDisplayName(profile.email.value)
                            )
                        )
                    } else {
                        dispatch(Msg.SetError(UiText.VerbatimString("User not found")))
                    }
                } catch (_: Exception) {
                    dispatch(Msg.SetError(UiText.VerbatimString("Failed to load")))
                } finally {
                    dispatch(Msg.SetRefreshing(false))
                }
            }
        }

        private fun updateDisplayName(newName: String) {
            dispatch(Msg.SetRefreshing(true))
            scope.launch {
                try {
                    // TODO: Call repository save
                    // sessionManager does not support updates yet
                    dispatch(Msg.SetDisplayName(UserDisplayName(newName)))
                    publish(ISettingsStore.Label.SettingsSaved)
                } catch (_: Exception) {
                    dispatch(Msg.SetError(UiText.VerbatimString("Failed to update")))
                } finally {
                    dispatch(Msg.SetRefreshing(false))
                }
            }
        }

        private fun logout() {
            scope.launch {
                try {
                    authenticationRepository.logout()
                } catch (_: Exception) {
                    // Ignore errors during logout
                }
            }
        }
    }

    private object SettingsReducer : Reducer<ISettingsStore.State, Msg> {
        override fun ISettingsStore.State.reduce(msg: Msg): ISettingsStore.State =
            when (msg) {
                is Msg.SetDisplayName -> copy(displayName = msg.name)
                is Msg.SetRefreshing -> copy(refreshing = msg.isRefreshing)
                is Msg.SetError -> copy(inlineError = msg.error)
            }
    }
}