package com.availelabs.availe.data.auth

import com.availelabs.availe.core.platform.PlatformActivity

data class PlatformAuthenticationConfig(
    val redirectUri: String,
    val postLogoutRedirectUri: String,
)

expect fun createPlatformAuthenticationConfig(activity: PlatformActivity): PlatformAuthenticationConfig