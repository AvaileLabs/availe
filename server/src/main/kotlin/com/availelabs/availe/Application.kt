package com.availelabs.availe

import com.availelabs.availe.controllers.ListingController
import kotlinx.serialization.Serializable
import org.http4k.contract.contract
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {
    val listingController = ListingController()

    val apiContract = contract {
        routes += listingController.createListing()
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