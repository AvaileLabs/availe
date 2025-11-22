package com.availelabs.availe

import com.availelabs.availe.controllers.ListingController
import com.availelabs.availe.domain.service.ListingService
import com.availelabs.availe.observability.CatchLensFailure
import com.availelabs.availe.repositories.listing.ListingRepository
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.EventListingCreator
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.ItemListingCreator
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.ServiceListingCreator
import com.availelabs.availe.setup.hikariSetup
import org.http4k.contract.contract
import org.http4k.core.then
import org.http4k.events.AutoMarshallingEvents
import org.http4k.events.EventFilters
import org.http4k.events.then
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
import org.http4k.format.KotlinxSerialization
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

    val events = EventFilters.AddTimestamp()
        .then(EventFilters.AddEventName())
        .then(AutoMarshallingEvents(KotlinxSerialization))

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
    val listingController = ListingController(listingService, events)

    val apiContract = contract {
        routes += listingController.listOfRoutes
    }

    val corsFilter = ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive)
    val lensFailureFilter = CatchLensFailure(events)

    val api = routes(
        "/api" bind apiContract,
    )

    val app = corsFilter
        .then(lensFailureFilter)
        .then(api)

    app.asServer(Helidon(9002)).start()
}