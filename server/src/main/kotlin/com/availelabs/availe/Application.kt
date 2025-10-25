package com.availelabs.availe

import com.availelabs.availe.controllers.ListingController
import com.availelabs.availe.repositories.listing.ListingRepository
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.EventListingCreator
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.ItemListingCreator
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.ServiceListingCreator
import com.availelabs.availe.setup.hikariSetup
import kotlinx.serialization.Serializable
import org.http4k.contract.contract
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

fun main() {
    val dataSource = hikariSetup()
    val dsl: DSLContext = DSL.using(dataSource, SQLDialect.POSTGRES)

    val itemCreator = ItemListingCreator(dsl)
    val eventCreator = EventListingCreator(dsl)
    val serviceCreator = ServiceListingCreator(dsl)

    val listingRepository = ListingRepository(
        itemCreator = itemCreator,
        eventCreator = eventCreator,
        serviceCreator = serviceCreator
    )
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