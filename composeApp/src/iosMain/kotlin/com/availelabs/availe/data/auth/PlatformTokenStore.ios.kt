package com.availelabs.availe.data.auth

import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.tokenstore.IosKeychainTokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore

@OptIn(ExperimentalOpenIdConnect::class)
actual fun createTokenStore(activity: PlatformActivity): TokenStore {
    return IosKeychainTokenStore()
}