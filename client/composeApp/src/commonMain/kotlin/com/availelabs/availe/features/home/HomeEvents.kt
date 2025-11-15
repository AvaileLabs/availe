package com.availelabs.availe.features.home

sealed interface HomeEvents {
    data object Refresh : HomeEvents
    data class ItemClicked(val itemId: String) : HomeEvents
    data class FavoriteClicked(val itemId: String) : HomeEvents
    data object LogoutClicked : HomeEvents
}