package com.availelabs.availe.auth

import com.availelabs.availe.domain.service.IUserProvisioningService
import com.availelabs.availe.observability.AuthenticationResult
import com.availelabs.availe.observability.AuthenticationTokenReceived
import org.http4k.core.Request
import org.http4k.events.Events

class RequestIdentityResolver(
    private val tokenVerifier: ITokenVerifier,
    private val userProvisioningService: IUserProvisioningService,
    private val events: Events
) {
    fun resolve(request: Request): IdentityResolutionResult {
        val authorizationHeader = request.header("Authorization")
        val bearerPrefix = "Bearer "

        if (authorizationHeader == null || !authorizationHeader.startsWith(bearerPrefix)) {
            return IdentityResolutionResult.MissingHeader
        }

        val token = authorizationHeader.substringAfter(bearerPrefix)
        events(AuthenticationTokenReceived(token.length))

        val verifiedKeycloakToken = tokenVerifier.verify(token)

        if (verifiedKeycloakToken != null) {
            val userId = userProvisioningService.GetOrProvisionUser(verifiedKeycloakToken)
            events(AuthenticationResult(isSuccessful = true, httpStatus = 200))

            val authenticatedUser = AuthenticatedUser(
                id = userId,
                email = verifiedKeycloakToken.email,
                displayName = verifiedKeycloakToken.displayName,
                avatarUrl = verifiedKeycloakToken.avatarUrl,
                jid = verifiedKeycloakToken.jid,
                subject = verifiedKeycloakToken.subject
            )
            return IdentityResolutionResult.Success(authenticatedUser)
        } else {
            events(
                AuthenticationResult(
                    isSuccessful = false,
                    httpStatus = 401,
                    reason = "Token verification failed"
                )
            )
            return IdentityResolutionResult.InvalidToken
        }
    }
}