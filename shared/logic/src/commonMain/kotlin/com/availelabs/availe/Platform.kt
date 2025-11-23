package com.availelabs.availe

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform