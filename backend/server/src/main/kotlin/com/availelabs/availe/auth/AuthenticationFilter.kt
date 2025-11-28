package com.availelabs.availe.auth

import com.availelabs.availe.observability.AuthenticationResult
import com.availelabs.availe.observability.AuthenticationTokenReceived
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.events.Events

class AuthenticationFilter(
    private val tokenVerifier: KeycloakTokenVerifier,
    private val events: Events
) : Filter {
    override fun invoke(next: HttpHandler): HttpHandler = { request ->
        val authorizationHeader = request.header("Authorization")
        val bearerPrefix = "Bearer "

        if (authorizationHeader != null && authorizationHeader.startsWith(bearerPrefix)) {
            val token = authorizationHeader.substringAfter(bearerPrefix)
            events(AuthenticationTokenReceived(token.length))

            val authenticatedUser = tokenVerifier.verify(token)

            if (authenticatedUser != null) {
                events(AuthenticationResult(isSuccessful = true, httpStatus = 200))
                val requestWithUser = SecurityContexts.authenticatedUserLens(authenticatedUser, request)
                next(requestWithUser)
            } else {
                events(
                    AuthenticationResult(
                        isSuccessful = false,
                        httpStatus = 401,
                        reason = "Token verification failed"
                    )
                )
                Response(Status.UNAUTHORIZED)
            }
        } else {
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