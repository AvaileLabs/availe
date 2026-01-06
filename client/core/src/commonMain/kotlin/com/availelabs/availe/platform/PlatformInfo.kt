package com.availelabs.availe.platform

enum class Platform {
    Android,
    Ios,
    Web,
    Desktop
}

expect object PlatformInfo {
    val current: Platform
}