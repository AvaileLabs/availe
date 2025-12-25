package com.availelabs.availe.auth

import com.availelabs.availe.platform.PlatformActivity

actual fun createPlatformAuthenticationConfig(activity: PlatformActivity): PlatformAuthenticationConfig {
    return PlatformAuthenticationConfig(
        redirectUri = AuthenticationConstants.REDIRECT_URI_ANDROID,
        postLogoutRedirectUri = AuthenticationConstants.POST_LOGOUT_REDIRECT_URI_ANDROID,
    )
}