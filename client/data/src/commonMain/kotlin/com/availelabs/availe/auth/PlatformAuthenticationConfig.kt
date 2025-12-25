package com.availelabs.availe.auth

import com.availelabs.availe.platform.PlatformActivity

data class PlatformAuthenticationConfig(
    val redirectUri: String,
    val postLogoutRedirectUri: String,
)

expect fun createPlatformAuthenticationConfig(activity: PlatformActivity): com.availelabs.availe.auth.PlatformAuthenticationConfig