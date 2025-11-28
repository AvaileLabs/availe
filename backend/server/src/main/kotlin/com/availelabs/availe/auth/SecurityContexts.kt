package com.availelabs.availe.auth

import com.availelabs.availe.domain.model.UserId
import org.http4k.lens.RequestKey

data class AuthenticatedUser(val id: UserId)

object SecurityContexts {
    val authenticatedUserLens = RequestKey.required<AuthenticatedUser>("authenticatedUser")
}