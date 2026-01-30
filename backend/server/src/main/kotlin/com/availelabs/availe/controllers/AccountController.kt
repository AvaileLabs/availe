package com.availelabs.availe.controllers

import com.availelabs.availe.auth.AuthenticationFilter
import com.availelabs.availe.auth.SecurityContexts
import com.availelabs.availe.domain.model.UserDisplayName
import com.availelabs.availe.dto.kreplica.PublicProfileDtoSchema
import com.availelabs.availe.repositories.user.IUserRepository
import com.availelabs.availe.service.IUserProfileService
import org.http4k.contract.ContractRoute
import org.http4k.contract.meta
import org.http4k.core.*
import org.http4k.format.KotlinxSerialization.auto
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class AccountController(
    private val userRepository: IUserRepository,
    private val userProfileService: IUserProfileService,
    private val authenticationFilter: AuthenticationFilter
) {
    private val responseBodyLens = Body.auto<PublicProfileDtoSchema.V1.Data>().toLens()

    private val requestBodyLens = Body.auto<PublicProfileDtoSchema.V1.Data>().toLens()

    val routes = listOf(
        getAccount(),
        updateAccount()
    )

    private fun getAccount(): ContractRoute {
        return "/account" meta {
            summary = "Get current account"
            returning(Status.OK, responseBodyLens to exampleProfile)
            returning(Status.UNAUTHORIZED)
            returning(Status.NOT_FOUND)
        } bindContract Method.GET to authenticationFilter.then { request ->
            val user = SecurityContexts.authenticatedUserLens(request)

            val profile = userRepository.GetUserById(user.id)

            if (profile != null) {
                val dto = PublicProfileDtoSchema.V1.Data(
                    id = profile.id,
                    displayName = profile.displayName,
                    avatarUrl = profile.avatarUrl,
                    jid = profile.jid
                )
                Response(Status.OK).with(responseBodyLens of dto)
            } else {
                Response(Status.NOT_FOUND)
            }
        }
    }

    private fun updateAccount(): ContractRoute {
        return "/account" meta {
            summary = "Update current account profile"
            receiving(requestBodyLens to exampleProfile)
            returning(Status.OK)
            returning(Status.UNAUTHORIZED)
        } bindContract Method.PATCH to authenticationFilter.then { request ->
            val user = SecurityContexts.authenticatedUserLens(request)
            val patch = requestBodyLens(request)

            userProfileService.UpdateProfile(
                id = user.id,
                displayName = patch.displayName,
                avatarUrl = patch.avatarUrl,
                subject = user.subject
            )

            Response(Status.OK)
        }
    }

    private val exampleProfile = PublicProfileDtoSchema.V1.Data(
        id = com.availelabs.availe.domain.model.UserId(kotlin.uuid.Uuid.parse("00000000-0000-0000-0000-000000000000")),
        displayName = UserDisplayName("Example User"),
        avatarUrl = null,
        jid = com.availelabs.availe.domain.model.Jid("user@localhost")
    )
}