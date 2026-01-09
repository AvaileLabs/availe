package com.availelabs.availe.navigation

import kotlinx.browser.window

actual object DeepLinkBuilder {
    actual fun build(route: DeepLinkRoute): String {
        val origin = window.location.origin.trimEnd('/')
        val cleanPath = route.relativePath.trimStart('/')
        return "$origin/$cleanPath"
    }
}