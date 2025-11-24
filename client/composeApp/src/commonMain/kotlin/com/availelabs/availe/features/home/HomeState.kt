package com.availelabs.availe.features.home

import com.availelabs.availe.ui.ContentState

data class ListingItem(
    val id: String,
    val title: String,
    val price: String,
    val imageUrl: String,
    val isFavorited: Boolean = false
)

data class HomeState(
    val listingsState: ContentState<List<ListingItem>> = ContentState.Idle
)