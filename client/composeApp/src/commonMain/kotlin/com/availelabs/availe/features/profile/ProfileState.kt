package com.availelabs.availe.features.profile

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
sealed interface UserProfile {
    val id: Uuid
    val email: String
    val pictureUrl: String?

    data class Named(
        override val id: Uuid,
        override val email: String,
        override val pictureUrl: String?,
        val name: String
    ) : UserProfile

    data class Anonymous(
        override val id: Uuid,
        override val email: String,
        override val pictureUrl: String?
    ) : UserProfile
}

data class ProfileState(
    val userProfile: UserProfile? = null,
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false
)