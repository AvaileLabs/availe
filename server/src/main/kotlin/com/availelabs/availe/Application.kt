package com.availelabs.availe

import kotlinx.serialization.Serializable
import org.http4k.contract.ContractRoute
import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {
    val apiContract = contract {
        routes += createListing()
    }

    val app = routes(
        "/api" bind apiContract,
    )

    app.asServer(Helidon(9000)).start()
}

@Serializable
sealed interface ListingTypeDto {
    @Serializable
    data object Item : ListingTypeDto

    @Serializable
    data object Event : ListingTypeDto

    @Serializable
    data object Service : ListingTypeDto
}

fun createListing(): ContractRoute {
    val bodyLens = Body.auto<ListingTypeDto>().toLens()

    return ApiRoutes.LISTINGS meta {
        summary = "Create a new listing."
        description = "Creates a new Item, Event, or Service listing."
        receiving(bodyLens to ListingTypeDto.Item)
        returning(OK, bodyLens to ListingTypeDto.Item)
    } bindContract Method.POST to { req ->
        val listingType = bodyLens(req)
        Response(OK).with(bodyLens of listingType)
    }
}