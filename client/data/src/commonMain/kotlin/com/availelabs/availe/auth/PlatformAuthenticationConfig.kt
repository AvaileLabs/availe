package com.availelabs.availe.auth

import com.availelabs.availe.platform.PlatformActivity

internal data class PlatformAuthenticationConfig(
    val redirectUri: String,
    val postLogoutRedirectUri: String,
)

internal expect fun createPlatformAuthenticationConfig(activity: PlatformActivity): PlatformAuthenticationConfig