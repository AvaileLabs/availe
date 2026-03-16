package com.availelabs.availe

import com.availelabs.availe.auth.AuthenticationFilter
import com.availelabs.availe.auth.KeycloakTokenVerifier
import com.availelabs.availe.auth.OptionalAuthenticationFilter
import com.availelabs.availe.auth.RequestIdentityResolver
import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.availe.controllers.*
import com.availelabs.availe.domain.service.UserProvisioningService
import com.availelabs.availe.observability.catchLensFailure
import com.availelabs.availe.repositories.event.EventRepository
import com.availelabs.availe.repositories.interaction.InteractionRepository
import com.availelabs.availe.repositories.listing.ListingRepository
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.EventListingCreator
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.ItemListingCreator
import com.availelabs.availe.repositories.listing.helpers.creators.implementations.ServiceListingCreator
import com.availelabs.availe.repositories.user.UserRepository
import com.availelabs.availe.service.EventService
import com.availelabs.availe.service.ListingService
import com.availelabs.availe.service.UserProfileService
import com.availelabs.availe.setup.hikariSetup
import com.availelabs.availe.setup.valkeyCacheServiceSetup
import org.http4k.contract.contract
import org.http4k.core.Method
import org.http4k.core.then
import org.http4k.events.AutoMarshallingEvents
import org.http4k.events.EventFilters
import org.http4k.events.then
import org.http4k.filter.AllowAll
import org.http4k.filter.CorsPolicy
import org.http4k.filter.OriginPolicy
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson
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
    val hikariDataSource = hikariSetup()
    val databaseContext: DSLContext = DSL.using(
        DefaultConfiguration()
            .set(hikariDataSource)
            .set(SQLDialect.POSTGRES)
            .set(Settings().withReturnDefaultOnUpdatableRecord(true))
    )

    val applicationEvents = EventFilters.AddTimestamp()
        .then(EventFilters.AddEventName())
        .then(AutoMarshallingEvents(Jackson, ::println))

    val userRepository = UserRepository(databaseContext)
    val interactionRepository = InteractionRepository(databaseContext)

    val itemListingCreator = ItemListingCreator(databaseContext)
    val eventListingCreator = EventListingCreator(databaseContext)
    val serviceListingCreator = ServiceListingCreator(databaseContext)

    val listingRepository = ListingRepository(
        databaseContext = databaseContext,
        itemListingCreator = itemListingCreator,
        eventListingCreator = eventListingCreator,
        serviceListingCreator = serviceListingCreator
    )

    val eventRepository = EventRepository(databaseContext)

    val valkeyCacheService = valkeyCacheServiceSetup()

    val listingService = ListingService(listingRepository, interactionRepository)
    val eventService = EventService(eventRepository, interactionRepository)

    val userProvisioningService = UserProvisioningService(valkeyCacheService, userRepository)
    val userProfileService = UserProfileService(userRepository, valkeyCacheService)

    val keycloakTokenVerifier = KeycloakTokenVerifier(applicationEvents)
    val requestIdentityResolver =
        RequestIdentityResolver(keycloakTokenVerifier, userProvisioningService, applicationEvents)

    val authenticationFilter = AuthenticationFilter(requestIdentityResolver, applicationEvents)
    val optionalAuthenticationFilter = OptionalAuthenticationFilter(requestIdentityResolver)

    val listingController =
        ListingController(listingService, applicationEvents, authenticationFilter, optionalAuthenticationFilter)
    val eventController = EventController(eventService, optionalAuthenticationFilter)
    val accountController = AccountController(userRepository, userProfileService, authenticationFilter)
    val usersController = UsersController(userRepository)
    val interactionController = InteractionController(interactionRepository, listingService, authenticationFilter)

    val apiContractRoutingHandler = contract {
        routes += listingController.listOfRoutes
        routes += eventController.listOfRoutes
        routes += accountController.routes
        routes += usersController.routes
        routes += interactionController.routes
    }

    val crossOriginResourceSharingPolicy = CorsPolicy(
        originPolicy = OriginPolicy.AllowAll(),
        headers = listOf("Authorization", "Content-Type", "Accept"),
        methods = Method.entries,
        credentials = true
    )

    val crossOriginResourceSharingFilter = ServerFilters.Cors(crossOriginResourceSharingPolicy)
    val lensFailureFilter = catchLensFailure(applicationEvents)

    val apiHandler = routes(
        "/api" bind apiContractRoutingHandler,
    )

    val applicationHttpHandler = crossOriginResourceSharingFilter
        .then(lensFailureFilter)
        .then(apiHandler)

    applicationHttpHandler.asServer(Helidon(SharedNetworkConstants.SERVER_API_PORT)).start()
}