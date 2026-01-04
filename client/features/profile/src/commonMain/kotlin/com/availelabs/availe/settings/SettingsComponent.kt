package com.availelabs.availe.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.availelabs.availe.repositories.IAuthenticationRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.ui.model.AppBarAction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

interface ISettingsComponent {
    val state: StateFlow<ISettingsStore.State>
    val actions: Value<List<AppBarAction>>
    fun onIntent(intent: ISettingsStore.Intent)
}

class SettingsComponent(
    componentContext: ComponentContext,
    storeFactory: StoreFactory,
    sessionManager: ISessionRepository,
    authenticationRepository: IAuthenticationRepository,
    private val onOpenUrl: (String) -> Unit
) : ISettingsComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        SettingsStoreFactory(
            storeFactory = storeFactory,
            sessionManager = sessionManager,
            authenticationRepository = authenticationRepository
        ).create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<ISettingsStore.State> = store.stateFlow

    private val _actions = MutableValue<List<AppBarAction>>(emptyList())
    override val actions: Value<List<AppBarAction>> = _actions

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        lifecycle.doOnDestroy { scope.cancel() }

        scope.launch {
            store.labels.collect { label ->
                when (label) {
                    is ISettingsStore.Label.SettingsSaved -> { /* Handle success toast? */
                    }

                    is ISettingsStore.Label.InfoMessage -> { /* Handle info toast? */
                    }

                    is ISettingsStore.Label.OpenUrl -> onOpenUrl(label.url)
                }
            }
        }
    }

    override fun onIntent(intent: ISettingsStore.Intent) {
        store.accept(intent)
    }
}