package com.availelabs.availe.auth

import com.availelabs.availe.observability.AuthenticationResult
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.events.Events

class AuthenticationFilter(
    private val requestIdentityResolver: RequestIdentityResolver,
    private val events: Events
) : Filter {
    override fun invoke(next: HttpHandler): HttpHandler = { request ->
        when (val identityResolutionResult = requestIdentityResolver.resolve(request)) {
            is IdentityResolutionResult.Success -> {
                val requestWithUser = SecurityContexts.authenticatedUserLens(
                    identityResolutionResult.authenticatedUser,
                    request
                )
                next(requestWithUser)
            }

            is IdentityResolutionResult.InvalidToken -> {
                Response(Status.UNAUTHORIZED)
            }

            is IdentityResolutionResult.MissingHeader -> {
                events(
                    AuthenticationResult(
                        isSuccessful = false,
                        httpStatus = 401,
                        reason = "Missing or invalid Authorization header"
                    )
                )
                Response(Status.UNAUTHORIZED)
            }
        }
    }
}