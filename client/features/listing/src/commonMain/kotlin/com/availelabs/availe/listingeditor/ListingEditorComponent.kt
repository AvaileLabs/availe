package com.availelabs.availe.listingeditor

import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.generic_save
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.ListingEditorIntent
import com.availelabs.availe.ListingEditorState
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.model.ToastMessage
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.services.IMessageService
import com.availelabs.availe.ui.model.ActionIcon
import com.availelabs.availe.ui.model.AppBarAction
import com.availelabs.availe.ui.model.UiText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

interface IListingEditorComponent {
    val state: StateFlow<ListingEditorState>
    val actions: Value<List<AppBarAction>>
    fun onIntent(intent: ListingEditorIntent)
    fun onBackClicked()
}

class ListingEditorComponent(
    componentContext: ComponentContext,
    private val listingId: ListingId?,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionRepository,
    private val messageService: IMessageService,
    private val onNavigateBack: () -> Unit
) : IListingEditorComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        ListingEditorStoreFactory(
            storeFactory = DefaultStoreFactory(),
            listingRepository = listingRepository,
            sessionManager = sessionManager,
            listingId = listingId
        ).create()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<ListingEditorState> = store.stateFlow

    private val _actions = MutableValue(
        listOf(
            AppBarAction(
                icon = ActionIcon.ADD, // Reusing ADD icon for Save until specific Save icon is available
                label = UiText.ResourceString(Res.string.generic_save),
                onClick = { store.accept(ListingEditorIntent.Submit) }
            )
        )
    )
    override val actions: Value<List<AppBarAction>> = _actions

    init {
        val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        lifecycle.doOnDestroy { scope.cancel() }

        scope.launch {
            store.labels.collect { label ->
                when (label) {
                    ListingEditorLabel.ListingCreated -> {
                        messageService.show(ToastMessage.Success("Listing created successfully"))
                        onNavigateBack()
                    }

                    ListingEditorLabel.ListingUpdated -> {
                        messageService.show(ToastMessage.Success("Listing updated successfully"))
                        onNavigateBack()
                    }

                    is ListingEditorLabel.ErrorOccurred -> {
                        messageService.show(ToastMessage.Error(label.error))
                    }
                }
            }
        }
    }

    override fun onIntent(intent: ListingEditorIntent) {
        store.accept(intent)
    }

    override fun onBackClicked() {
        onNavigateBack()
    }
}