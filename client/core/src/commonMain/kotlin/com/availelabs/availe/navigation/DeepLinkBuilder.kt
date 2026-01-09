package com.availelabs.availe.navigation

expect object DeepLinkBuilder {
    fun build(route: DeepLinkRoute): String
}