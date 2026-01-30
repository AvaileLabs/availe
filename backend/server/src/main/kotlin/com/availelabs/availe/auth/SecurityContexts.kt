package com.availelabs.availe.auth

import com.availelabs.availe.domain.model.*
import org.http4k.lens.RequestKey

data class AuthenticatedUser(
    val id: UserId,
    val email: Email,
    val displayName: UserDisplayName,
    val avatarUrl: ImageUrl?,
    val jid: Jid?,
    val subject: String
)

object SecurityContexts {
    val authenticatedUserLens = RequestKey.required<AuthenticatedUser>("authenticatedUser")
}