package com.availelabs.availe

import kotlinx.serialization.Serializable
import org.http4k.contract.ContractRoute
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
    val app = routes(
        "route" bind Method.GET to { Response(OK).body("Hello world!") }
    )

    val server = app.asServer(Helidon(9000)).start()
}

@Serializable
sealed interface ListingType {
    @Serializable
    data object Item : ListingType

    @Serializable
    data object Event : ListingType

    @Serializable
    data object Service : ListingType
}

fun createListing(): ContractRoute {
    val bodyLens = Body.auto<ListingType>().toLens()

    return "listings" meta {
        summary = "Create a new listing."
        description = "Creates a new Item, Event, or Service listing."
        receiving(bodyLens to ListingType.Item)
    } bindContract Method.POST to { req ->
        val listingType = bodyLens(req)
        Response(OK).with(bodyLens of listingType)
    }
}