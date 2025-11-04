package com.availelabs.availe.data.auth

import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.domain.auth.AuthenticationConstants

actual fun createPlatformAuthenticationConfig(activity: PlatformActivity): PlatformAuthenticationConfig {
    return PlatformAuthenticationConfig(
        redirectUri = AuthenticationConstants.REDIRECT_URI_IOS,
        postLogoutRedirectUri = AuthenticationConstants.POST_LOGOUT_REDIRECT_URI_IOS,
    )
}