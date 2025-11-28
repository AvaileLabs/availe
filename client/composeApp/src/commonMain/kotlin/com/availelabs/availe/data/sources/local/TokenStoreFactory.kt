package com.availelabs.availe.data.sources.local

import com.availelabs.availe.core.platform.PlatformActivity
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore

@OptIn(ExperimentalOpenIdConnect::class)
expect fun createTokenStore(activity: PlatformActivity): TokenStore