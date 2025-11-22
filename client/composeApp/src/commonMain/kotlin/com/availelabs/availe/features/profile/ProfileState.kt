package com.availelabs.availe.features.profile

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class UserProfile(
    val id: Uuid,
    val name: String,
    val email: String,
    val pictureUrl: String? = null
)

data class ProfileState(
    val userProfile: UserProfile? = null,
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false
)