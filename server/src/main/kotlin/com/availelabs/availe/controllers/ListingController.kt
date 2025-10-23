@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.controllers

import com.availelabs.availe.ApiRoutes
import com.availelabs.availe.ListingTypeDto
import org.http4k.contract.ContractRoute
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.KotlinxSerialization.auto
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

class ListingController {
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
}