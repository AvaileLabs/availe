package com.availelabs.availe.extensions

import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*

internal suspend fun HttpResponse.ensureSuccess() {
    val statusCode = this.status.value
    if (this.status.isSuccess()) {
        return
    }

    val cachedBody = try {
        this.bodyAsText()
    } catch (_: Exception) {
        "<no body>"
    }

    when (statusCode) {
        in 300..399 -> throw RedirectResponseException(this, cachedBody)
        in 400..499 -> throw ClientRequestException(this, cachedBody)
        in 500..599 -> throw ServerResponseException(this, cachedBody)
        else -> throw IllegalStateException("Unexpected status code: $statusCode - $cachedBody")
    }
}