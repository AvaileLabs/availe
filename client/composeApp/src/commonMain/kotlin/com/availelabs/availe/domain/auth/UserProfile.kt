package com.availelabs.availe.domain.auth

import com.availelabs.availe.domain.model.Email
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.UserId

data class UserProfile(
    val id: UserId,
    val email: Email,
    val jid: Jid
)

data class ProfileState(
    val userProfile: UserProfile? = null,
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val initializationError: String? = null
)