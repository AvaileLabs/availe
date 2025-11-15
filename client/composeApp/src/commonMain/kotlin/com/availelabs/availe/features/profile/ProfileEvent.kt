package com.availelabs.availe.features.profile

sealed interface ProfileEvents {
    data object LoginClicked : ProfileEvents
    data object LogoutClicked : ProfileEvents
    data object ManageAccountClicked : ProfileEvents
}