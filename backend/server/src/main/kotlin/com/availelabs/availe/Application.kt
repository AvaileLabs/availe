package com.availelabs.availe

import com.availelabs.availe.controllers.ListingController
import com.availelabs.availe.repositories.listing.ListingRepository
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.EventListingCreator
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.ItemListingCreator
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.ServiceListingCreator
import com.availelabs.availe.services.ListingService
import com.availelabs.availe.setup.hikariSetup
import org.http4k.contract.contract
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration

internal fun main() {
    val dataSource = hikariSetup()
    val dsl: DSLContext = DSL.using(
        DefaultConfiguration()
            .set(dataSource)
            .set(SQLDialect.POSTGRES)
            .set(Settings().withReturnDefaultOnUpdatableRecord(true))
    )

    val itemCreator = ItemListingCreator(dsl)
    val eventCreator = EventListingCreator(dsl)
    val serviceCreator = ServiceListingCreator(dsl)

    val listingRepository = ListingRepository(
        dsl = dsl,
        itemCreator = itemCreator,
        eventCreator = eventCreator,
        serviceCreator = serviceCreator
    )
    val listingService = ListingService(listingRepository)
    val listingController = ListingController(listingService)

    val apiContract = contract {
        routes += listingController.listOfRoutes
    }

    val app = routes(
        "/api" bind apiContract,
    )

    app.asServer(Helidon(9002)).start()
}