package com.availelabs.availe.ui.screens.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.availelabs.availe.ui.events.HomeScreenEvents
import com.availelabs.availe.ui.utils.ContentLoader
import com.availelabs.availe.ui.utils.ContentState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

interface IHomeScreenComponent {
    val uiState: Value<HomeUiState>
    fun onEvent(event: HomeScreenEvents)
}

class HomeScreenComponent(
    componentContext: ComponentContext,
    private val onLogout: () -> Unit
) : IHomeScreenComponent, ComponentContext by componentContext {

    private val scope = componentContext.coroutineScope(Dispatchers.Main.immediate)

    private val listingsLoader = ContentLoader(
        scope = scope,
        fetcher = { fetchListings() }
    )

    private val _uiState = MutableValue(HomeUiState())
    override val uiState: Value<HomeUiState> = _uiState

    init {
        listingsLoader.state.subscribe { newListingsState ->
            _uiState.update { it.copy(listingsState = newListingsState) }
        }

        listingsLoader.load()
    }

    override fun onEvent(event: HomeScreenEvents) {
        when (event) {
            is HomeScreenEvents.Refresh -> listingsLoader.refresh()
            is HomeScreenEvents.ItemClicked -> {
                println("Item clicked: ${event.itemId}")
            }

            is HomeScreenEvents.FavoriteClicked -> toggleFavorite(event.itemId)
            is HomeScreenEvents.LogoutClicked -> onLogout()
        }
    }

    private suspend fun fetchListings(): List<ListingItem> {
        delay(1000)
        return (1..20).map {
            ListingItem(
                id = it.toString(),
                title = "Cool Item $it",
                price = "$${(it * 10..it * 20).random()}",
                imageUrl = "https://picsum.photos/seed/$it/300/300",
                isFavorited = (it % 4 == 0)
            )
        }
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