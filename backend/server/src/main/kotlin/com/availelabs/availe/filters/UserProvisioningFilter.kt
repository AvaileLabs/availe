package com.availelabs.availe.filters

import com.availelabs.availe.auth.VerifiedKeycloakToken
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.service.IUserProvisioningService
import org.http4k.core.*
import org.http4k.lens.BiDiLens

class UserProvisioningFilter(
    private val userProvisioningService: IUserProvisioningService,
    private val verifiedKeycloakTokenLens: BiDiLens<Request, VerifiedKeycloakToken>,
    private val userIdLens: BiDiLens<Request, UserId>
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler = { request ->
        try {
            val verifiedKeycloakToken = verifiedKeycloakTokenLens(request)
            val userId = userProvisioningService.GetOrProvisionUser(verifiedKeycloakToken)

            val requestWithUser = request.with(userIdLens of userId)
            next(requestWithUser)
        } catch (e: Exception) {
            Response(Status.INTERNAL_SERVER_ERROR).body("User provisioning failed: ${e.message}")
        }
    }
}