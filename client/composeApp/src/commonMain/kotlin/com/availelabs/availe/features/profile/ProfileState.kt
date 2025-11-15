package com.availelabs.availe.features.profile

data class UserProfile(
    val name: String,
    val email: String,
    val pictureUrl: String? = null
)

data class ProfileState(
    val userProfile: UserProfile? = null,
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false
)