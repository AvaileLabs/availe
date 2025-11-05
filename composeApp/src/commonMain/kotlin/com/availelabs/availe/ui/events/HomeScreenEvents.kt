package com.availelabs.availe.ui.events

sealed interface HomeScreenEvents {
    data object Refresh : HomeScreenEvents
    data class ItemClicked(val itemId: String) : HomeScreenEvents
    data class FavoriteClicked(val itemId: String) : HomeScreenEvents
    data object LogoutClicked : HomeScreenEvents
}