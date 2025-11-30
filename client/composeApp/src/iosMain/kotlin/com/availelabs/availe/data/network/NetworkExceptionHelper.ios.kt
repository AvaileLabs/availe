package com.availelabs.availe.data.network

import io.ktor.client.engine.darwin.*

actual fun Throwable.isPlatformSpecificNetworkError(): Boolean {
    return this is DarwinHttpRequestException
}