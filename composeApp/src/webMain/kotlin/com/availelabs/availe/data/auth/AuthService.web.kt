package com.availelabs.availe.data.auth

import com.availelabs.availe.domain.AuthConstants

actual fun createPlatformAuthConfig(activity: PlatformActivity): PlatformAuthConfig {
    return PlatformAuthConfig(
        redirectUri = AuthConstants.REDIRECT_URI_WEB,
        postLogoutRedirectUri = AuthConstants.POST_LOGOUT_REDIRECT_URI_WEB,
    )
}