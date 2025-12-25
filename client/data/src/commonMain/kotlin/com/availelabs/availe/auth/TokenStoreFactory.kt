package com.availelabs.availe.auth

import com.availelabs.availe.platform.PlatformActivity
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore

@OptIn(ExperimentalOpenIdConnect::class)
internal expect fun createTokenStore(activity: PlatformActivity): TokenStore