package com.availelabs.availe.controllers

import com.availelabs.availe.ApiParameters
import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.auth.AuthenticationFilter
import com.availelabs.availe.auth.SecurityContexts
import com.availelabs.availe.domain.model.InteractionAction
import com.availelabs.availe.domain.model.InteractionType
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.dto.kreplica.InteractionRequestDtoSchema
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoData
import com.availelabs.availe.mappers.toDataDto
import com.availelabs.availe.repositories.interaction.IInteractionRepository
import com.availelabs.availe.service.IListingService
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.*
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.lens.Path
import org.http4k.lens.uuid
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class InteractionController(
    private val interactionRepository: IInteractionRepository,
    private val listingService: IListingService,
    private val authenticationFilter: AuthenticationFilter
) {
    private val listingIdPath =
        Path.uuid().map({ ListingId(it.toKotlinUuid()) }, { it.value.toJavaUuid() }).of(ApiParameters.LISTING_ID)

    private val requestBodyLens = Body.auto<InteractionRequestDtoSchema.V1.Data>().toLens()
    private val responseBodyLens = Body.auto<TypeV1ListingDtoData>().toLens()

    val routes = listOf(
        updateInteraction()
    )

    private fun updateInteraction(): ContractRoute {
        val exampleRequest = InteractionRequestDtoSchema.V1.Data(
            action = InteractionAction.Add,
            type = InteractionType.Favorite
        )

        return ApiRoutes.LISTINGS / listingIdPath / "interaction" meta {
            summary = "Update user interaction with a listing"
            description = "Add or remove an interaction (e.g., Favorite) for the authenticated user."
            receiving(requestBodyLens to exampleRequest)
            returning(Status.OK)
            returning(Status.UNAUTHORIZED)
            returning(Status.NOT_FOUND)
        } bindContract Method.PATCH to { listingId, _ ->
            authenticationFilter.then { request ->
                val user = SecurityContexts.authenticatedUserLens(request)
                val body = requestBodyLens(request)

                when (body.action) {
                    InteractionAction.Add -> {
                        interactionRepository.addInteraction(user.id, listingId, body.type)
                    }

                    InteractionAction.Remove -> {
                        interactionRepository.removeInteraction(user.id, listingId, body.type)
                    }
                }

                when (val result = listingService.getListingById(listingId, user.id)) {
                    is FindResult.Found -> {
                        val responseDataTransferObject = result.value.toDataDto()
                        Response(Status.OK).with(responseBodyLens of responseDataTransferObject)
                    }

                    is FindResult.NotFound -> {
                        Response(Status.NOT_FOUND)
                    }
                }
            }
        }
    }
}