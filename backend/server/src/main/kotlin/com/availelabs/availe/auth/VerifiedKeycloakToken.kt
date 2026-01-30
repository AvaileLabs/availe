package com.availelabs.availe.auth

import com.availelabs.availe.domain.model.*

data class VerifiedKeycloakToken(
    val userId: UserId,
    val email: Email,
    val displayName: UserDisplayName,
    val avatarUrl: ImageUrl?,
    val jid: Jid?
)