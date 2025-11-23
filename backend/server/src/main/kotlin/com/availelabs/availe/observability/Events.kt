package com.availelabs.availe.observability

import kotlinx.serialization.Serializable
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.events.Event
import org.http4k.events.Events
import org.http4k.lens.LensFailure

@Serializable
data class SerializationError(
    val uri: String,
    val body: String,
    val message: String,
    val failures: List<String>
) : Event

@Serializable
data class ListingCreated(
    val listingId: String,
    val publisherId: String
) : Event

fun catchLensFailure(events: Events): Filter = Filter { next ->
    { req ->
        try {
            next(req)
        } catch (e: LensFailure) {
            events(
                SerializationError(
                    uri = req.uri.toString(),
                    body = req.bodyString(),
                    message = e.message ?: "Lens failure",
                    failures = e.failures.map { it.toString() }
                )
            )
            Response(Status.BAD_REQUEST)
        }
    }
}