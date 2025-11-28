package com.availelabs.availe.auth

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status

class AuthenticationFilter(
    private val tokenVerifier: KeycloakTokenVerifier
) : Filter {
    override fun invoke(next: HttpHandler): HttpHandler = { request ->
        val authorizationHeader = request.header("Authorization")
        val bearerPrefix = "Bearer "

        if (authorizationHeader != null && authorizationHeader.startsWith(bearerPrefix)) {
            val token = authorizationHeader.substringAfter(bearerPrefix)
            val authenticatedUser = tokenVerifier.verify(token)

            if (authenticatedUser != null) {
                val requestWithUser = SecurityContexts.authenticatedUserLens(authenticatedUser, request)
                next(requestWithUser)
            } else {
                Response(Status.UNAUTHORIZED)
            }
        } else {
            Response(Status.UNAUTHORIZED)
        }
    }
}