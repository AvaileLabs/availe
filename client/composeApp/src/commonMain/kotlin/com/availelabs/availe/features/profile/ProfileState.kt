package com.availelabs.availe.features.profile

import com.availelabs.availe.domain.model.Email
import com.availelabs.availe.domain.model.ImageUrl
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.model.UserName

sealed interface UserProfile {
    val id: UserId
    val email: Email
    val pictureUrl: ImageUrl?

    data class Named(
        override val id: UserId,
        override val email: Email,
        override val pictureUrl: ImageUrl?,
        val name: UserName
    ) : UserProfile

    data class Anonymous(
        override val id: UserId,
        override val email: Email,
        override val pictureUrl: ImageUrl?
    ) : UserProfile
}

data class ProfileState(
    val userProfile: UserProfile? = null,
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val initializationError: String? = null
)