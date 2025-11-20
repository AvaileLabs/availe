package com.availelabs.availe

import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.webhistory.withWebHistory

@OptIn(ExperimentalDecomposeApi::class)
fun main() {
    withWebHistory { stateKeeper, deepLink ->
        startWebApp(deepLink = deepLink, stateKeeper = stateKeeper)
    }
}