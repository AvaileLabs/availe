package com.availelabs.availe.listingeditor

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.pages.*
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.arkivanov.mvikotlin.core.rx.Observer
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.ListingEditorIntent
import com.availelabs.availe.ListingEditorState
import com.availelabs.availe.decompose.asValue
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.model.ToastMessage
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.services.IMessageService
import com.availelabs.availe.toDraft
import com.availelabs.availe.ui.model.ActionIcon
import com.availelabs.availe.ui.model.AppBarAction
import com.availelabs.availe.ui.model.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface IListingEditorComponent {
    val state: Value<ListingEditorState>
    val actions: Value<List<AppBarAction>>
    val pages: Value<ChildPages<ListingEditorPageConfig, ListingEditorPage>>
    fun onTitleChanged(title: String)
    fun onDescriptionChanged(description: String)
    fun onPriceAmountChanged(amount: String)
    fun onCurrencySelected(currency: SupportedCurrency)
    fun onTypeSelected(type: ListingCategory)
    fun onImageUrlChanged(url: String)
    fun onPageSelected(index: Int)
    fun onPublishClicked()
    fun onConfirmPublish()
    fun onCancelPublish()
    fun onDiscardDraftClicked()
    fun onConfirmDiscardDraft()
    fun onCancelDiscardDraft()
    fun onBackClicked()
}

sealed interface ListingEditorPage {
    data object Type : ListingEditorPage
    data object Basics : ListingEditorPage
    data object Pricing : ListingEditorPage
    data object Media : ListingEditorPage
    data object Review : ListingEditorPage
}

@Serializable
sealed interface ListingEditorPageConfig {
    @Serializable
    data object Type : ListingEditorPageConfig

    @Serializable
    data object Basics : ListingEditorPageConfig

    @Serializable
    data object Pricing : ListingEditorPageConfig

    @Serializable
    data object Media : ListingEditorPageConfig

    @Serializable
    data object Review : ListingEditorPageConfig
}

@OptIn(ExperimentalDecomposeApi::class, ExperimentalCoroutinesApi::class)
class ListingEditorComponent(
    componentContext: ComponentContext,
    private val listingId: ListingId?,
    private val listingRepository: IListingRepository,
    sessionManager: ISessionRepository,
    private val messageService: IMessageService,
    private val navigator: IListingEditorNavigator
) : IListingEditorComponent, ComponentContext by componentContext {

    private val store: IListingEditorStore = ListingEditorStoreFactory(
        storeFactory = DefaultStoreFactory(),
        listingRepository = listingRepository,
        sessionManager = sessionManager,
        listingId = listingId
    ).create()

    override val state: Value<ListingEditorState> = store.stateFlow.asValue(lifecycle)

    private val pagesNavigation = PagesNavigation<ListingEditorPageConfig>()

    override val pages: Value<ChildPages<ListingEditorPageConfig, ListingEditorPage>> = childPages(
        source = pagesNavigation,
        serializer = ListingEditorPageConfig.serializer(),
        initialPages = {
            Pages(
                items = listOf(
                    ListingEditorPageConfig.Type,
                    ListingEditorPageConfig.Basics,
                    ListingEditorPageConfig.Pricing,
                    ListingEditorPageConfig.Media,
                    ListingEditorPageConfig.Review
                ),
                selectedIndex = 0
            )
        },
        childFactory = { config, _ ->
            when (config) {
                ListingEditorPageConfig.Type -> ListingEditorPage.Type
                ListingEditorPageConfig.Basics -> ListingEditorPage.Basics
                ListingEditorPageConfig.Pricing -> ListingEditorPage.Pricing
                ListingEditorPageConfig.Media -> ListingEditorPage.Media
                ListingEditorPageConfig.Review -> ListingEditorPage.Review
            }
        }
    )

    override val actions: Value<List<AppBarAction>> = MutableValue(
        listOf(
            AppBarAction(
                icon = ActionIcon.DELETE,
                label = UiText.VerbatimString("Discard"),
                onClick = { store.accept(ListingEditorIntent.AskToDiscardDraft) }
            )
        )
    )

    init {
        lifecycle.doOnResume {
            if (listingId == null) {
                // If creating new listing, check for draft
                val scope = CoroutineScope(Dispatchers.Main)
                scope.launch {
                    val draft = listingRepository.getDraft()
                    if (draft != null && !store.state.isEditing) {
                        store.accept(ListingEditorIntent.RestoreDraft(draft))
                        pagesNavigation.select(draft.stepIndex)
                    }
                }
            }
        }

        lifecycle.doOnPause {
            saveDraft()
        }

        store.labels(object : Observer<ListingEditorLabel> {
            override fun onNext(value: ListingEditorLabel) {
                when (value) {
                    is ListingEditorLabel.ListingCreated, is ListingEditorLabel.ListingUpdated -> {
                        messageService.show(ToastMessage.Success("Listing published successfully"))
                        navigator.navigateBack()
                    }

                    is ListingEditorLabel.ListingCleared -> {
                        messageService.show(ToastMessage.Info("Draft discarded"))
                        navigator.navigateBack()
                    }

                    is ListingEditorLabel.DraftRestored -> {
                        // handled in onResume logic for navigation sync
                    }

                    is ListingEditorLabel.ErrorOccurred -> {
                        // handled in UI via state
                    }
                }
            }

            override fun onComplete() {
                // no-op
            }
        })
    }

    private fun saveDraft() {
        if (listingId == null && !store.state.isSubmitting) {
            val scope = CoroutineScope(Dispatchers.Main)
            scope.launch {
                val currentIndex = pages.value.selectedIndex
                val draft = store.state.toDraft(currentIndex)
                listingRepository.saveDraft(draft)
            }
        }
    }

    override fun onTitleChanged(title: String) {
        store.accept(ListingEditorIntent.ChangeTitle(ListingTitle(title)))
    }

    override fun onDescriptionChanged(description: String) {
        store.accept(ListingEditorIntent.ChangeDescription(ListingDescription(description)))
    }

    override fun onPriceAmountChanged(amount: String) {
        store.accept(ListingEditorIntent.ChangePrice(PriceAmount(amount)))
    }

    override fun onCurrencySelected(currency: SupportedCurrency) {
        store.accept(ListingEditorIntent.ChangeCurrency(currency))
    }

    override fun onTypeSelected(type: ListingCategory) {
        store.accept(ListingEditorIntent.ChangeType(type))
    }

    override fun onImageUrlChanged(url: String) {
        store.accept(ListingEditorIntent.ChangeImage(ImageUrl(url)))
    }

    override fun onPageSelected(index: Int) {
        pagesNavigation.select(index)
        saveDraft()
    }

    override fun onPublishClicked() {
        store.accept(ListingEditorIntent.AskToPublish)
    }

    override fun onConfirmPublish() {
        store.accept(ListingEditorIntent.ConfirmPublish)
    }

    override fun onCancelPublish() {
        store.accept(ListingEditorIntent.CancelPublish)
    }

    override fun onDiscardDraftClicked() {
        store.accept(ListingEditorIntent.AskToDiscardDraft)
    }

    override fun onConfirmDiscardDraft() {
        store.accept(ListingEditorIntent.ConfirmDiscardDraft)
    }

    override fun onCancelDiscardDraft() {
        store.accept(ListingEditorIntent.CancelDiscardDraft)
    }

    override fun onBackClicked() {
        navigator.navigateBack()
    }
}