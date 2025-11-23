@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.availelabs.availe.data.network.IApiClient
import com.availelabs.availe.domain.logic.toDisplayString
import com.availelabs.availe.domain.model.Listing
import com.availelabs.ui.utils.ContentLoader
import com.availelabs.ui.utils.ContentState
import kotlinx.coroutines.Dispatchers
import kotlin.uuid.ExperimentalUuidApi

interface IHomeComponent {
    val uiState: Value<HomeState>
    fun onEvent(event: HomeEvents)
}

class HomeComponent(
    componentContext: ComponentContext,
    private val apiClient: IApiClient,
    private val onLogout: () -> Unit
) : IHomeComponent, ComponentContext by componentContext {

    private val scope = componentContext.coroutineScope(Dispatchers.Main.immediate)

    private val listingsLoader = ContentLoader(
        scope = scope,
        fetcher = { fetchListings() }
    )

    private val _uiState = MutableValue(HomeState())
    override val uiState: Value<HomeState> = _uiState

    init {
        listingsLoader.state.subscribe { newListingsState ->
            _uiState.update { it.copy(listingsState = newListingsState) }
        }

        listingsLoader.load()
    }

    override fun onEvent(event: HomeEvents) {
        when (event) {
            is HomeEvents.Refresh -> listingsLoader.refresh()
            is HomeEvents.ItemClicked -> {
                println("Item clicked: ${event.itemId}")
            }

            is HomeEvents.FavoriteClicked -> toggleFavorite(event.itemId)
            is HomeEvents.LogoutClicked -> onLogout()
        }
    }

    private suspend fun fetchListings(): List<ListingItem> {
        val domainListings = apiClient.getListings()
        return domainListings.map { it.toListingItem() }
    }

    private fun toggleFavorite(itemId: String) {
        val currentListings = (uiState.value.listingsState as? ContentState.Content)?.data ?: return

        val updatedListings = currentListings.map {
            if (it.id == itemId) {
                it.copy(isFavorited = !it.isFavorited)
            } else {
                it
            }
        }

        _uiState.update {
            it.copy(listingsState = ContentState.Content(updatedListings))
        }
    }
}

private fun Listing.toListingItem(): ListingItem {
    return ListingItem(
        id = this.id.value.toString(),
        title = this.title.value,
        price = this.price.toDisplayString(),
        imageUrl = this.imageLinks.firstOrNull()?.value ?: "",
        isFavorited = false
    )
}