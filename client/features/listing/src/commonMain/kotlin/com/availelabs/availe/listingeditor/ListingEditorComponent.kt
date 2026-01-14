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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface IListingEditorComponent {
    val state: StateFlow<ListingEditorState>
    val wizardState: StateFlow<ListingEditorWizardState>
    val actions: Value<List<AppBarAction>>
    fun onIntent(intent: ListingEditorIntent)
    fun onWizardIntent(intent: ListingEditorWizardIntent)
    fun onBackClicked()
}

class ListingEditorComponent(
    componentContext: ComponentContext,
    private val listingId: ListingId?,
    private val listingRepository: IListingRepository,
    private val sessionManager: ISessionRepository,
    private val messageService: IMessageService,
    private val navigator: IListingEditorNavigator
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

    private val mutableWizardState = MutableStateFlow(
        ListingEditorWizardState.initial(isEditingFlow = listingId != null)
    )
    override val wizardState: StateFlow<ListingEditorWizardState> = mutableWizardState

    private val _actions = MutableValue(emptyList<AppBarAction>())
    override val actions: Value<List<AppBarAction>> = _actions

    init {
        val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        lifecycle.doOnDestroy { scope.cancel() }

        updateActions(mutableWizardState.value, store.state)

        scope.launch {
            store.labels.collect { label ->
                when (label) {
                    ListingEditorLabel.ListingCreated -> {
                        messageService.show(ToastMessage.Success("Listing created successfully"))
                        navigator.navigateBack()
                    }

                    ListingEditorLabel.ListingUpdated -> {
                        messageService.show(ToastMessage.Success("Listing updated successfully"))
                        navigator.navigateBack()
                    }

                    ListingEditorLabel.ListingCleared -> {
                        mutableWizardState.value = ListingEditorWizardState.initial(isEditingFlow = listingId != null)
                        messageService.show(ToastMessage.Info("Form cleared"))
                    }

                    is ListingEditorLabel.ErrorOccurred -> {
                        messageService.show(ToastMessage.Error(label.error))
                    }

                    ListingEditorLabel.DraftRestored -> {
                        messageService.show(ToastMessage.Info("Draft restored"))
                        // Re-evaluate wizard state if draft loaded a category
                        val currentEditorState = store.state
                        val currentWizardState = mutableWizardState.value
                        if (currentWizardState.selectedCategory != currentEditorState.type && !currentWizardState.isEditingFlow) {
                            mutableWizardState.value =
                                currentWizardState.copy(selectedCategory = currentEditorState.type)
                        }
                    }
                }
            }
        }

        scope.launch {
            state.collect { editorState ->
                if (listingId != null) {
                    val currentWizardState = mutableWizardState.value
                    if (currentWizardState.selectedCategory != editorState.type) {
                        mutableWizardState.value = currentWizardState.copy(selectedCategory = editorState.type)
                    }
                }
                updateActions(mutableWizardState.value, editorState)
            }
        }

        scope.launch {
            wizardState.collect {
                updateActions(it, store.state)
            }
        }
    }

    private fun updateActions(wizardState: ListingEditorWizardState, editorState: ListingEditorState) {
        val actionsList = mutableListOf<AppBarAction>()

        val isDirty = editorState.title.value.isNotEmpty() ||
                editorState.description.value.isNotEmpty() ||
                editorState.priceAmount.value.isNotEmpty() ||
                editorState.imageUrl.value.isNotEmpty()

        if (isDirty) {
            actionsList.add(
                AppBarAction(
                    icon = ActionIcon.DELETE,
                    label = UiText.VerbatimString("Clear"),
                    onClick = { store.accept(ListingEditorIntent.AskToClear) }
                )
            )
        }

        if (wizardState.currentStep == ListingEditorWizardStep.Review) {
            actionsList.add(
                AppBarAction(
                    icon = ActionIcon.ADD,
                    label = UiText.ResourceString(Res.string.generic_save),
                    onClick = { store.accept(ListingEditorIntent.Submit) }
                )
            )
        }

        _actions.value = actionsList
    }

    override fun onIntent(intent: ListingEditorIntent) {
        store.accept(intent)
    }

    override fun onWizardIntent(intent: ListingEditorWizardIntent) {
        val reduction = reduceListingEditorWizard(
            wizardState = mutableWizardState.value,
            editorState = store.state,
            intent = intent
        )

        mutableWizardState.value = reduction.state
        if (reduction.editorIntent != null) {
            store.accept(reduction.editorIntent)
        }
    }

    override fun onBackClicked() {
        navigator.navigateBack()
    }
}