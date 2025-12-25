package com.availelabs.availe.auth

import com.availelabs.availe.platform.PlatformActivity


internal actual fun createPlatformAuthenticationConfig(activity: PlatformActivity): PlatformAuthenticationConfig {
    return PlatformAuthenticationConfig(
        redirectUri = AuthenticationConstants.REDIRECT_URI_WEB,
        postLogoutRedirectUri = AuthenticationConstants.POST_LOGOUT_REDIRECT_URI_WEB,
    )
}