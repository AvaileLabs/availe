package com.availelabs.availe.data.auth

import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore

@OptIn(ExperimentalOpenIdConnect::class)
expect fun createTokenStore(activity: PlatformActivity): TokenStore