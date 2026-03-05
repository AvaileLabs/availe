package com.availelabs.availe.auth

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status

class OptionalAuthenticationFilter(
    private val requestIdentityResolver: RequestIdentityResolver
) : Filter {
    override fun invoke(next: HttpHandler): HttpHandler = { request ->
        when (val identityResolutionResult = requestIdentityResolver.resolve(request)) {
            is IdentityResolutionResult.Success -> {
                val requestWithUser = SecurityContexts.optionalAuthenticatedUserLens(
                    identityResolutionResult.authenticatedUser,
                    request
                )
                next(requestWithUser)
            }

            is IdentityResolutionResult.InvalidToken -> {
                Response(Status.UNAUTHORIZED)
            }

            is IdentityResolutionResult.MissingHeader -> {
                next(request)
            }
        }
    }
}