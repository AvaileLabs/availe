package com.availelabs.availe.auth

import kotlinx.browser.window
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlow
import org.publicvalue.multiplatform.oidc.flows.EndSessionFlow
import org.publicvalue.multiplatform.oidc.preferences.Preferences
import org.publicvalue.multiplatform.oidc.preferences.PreferencesFactory
import org.publicvalue.multiplatform.oidc.types.AuthCodeRequest
import org.publicvalue.multiplatform.oidc.types.EndSessionRequest

@OptIn(ExperimentalOpenIdConnect::class)
class WebRedirectCodeAuthFlowFactory : CodeAuthFlowFactory {

    private val preferencesFactory = PreferencesFactory()

    override fun createAuthFlow(client: OpenIdConnectClient): CodeAuthFlow {
        return WebRedirectCodeAuthFlow(client, preferencesFactory.create())
    }

    override fun createEndSessionFlow(client: OpenIdConnectClient): EndSessionFlow {
        return WebRedirectEndSessionFlow(client, preferencesFactory.create())
    }
}

@OptIn(ExperimentalOpenIdConnect::class)
class WebRedirectCodeAuthFlow(
    override val client: OpenIdConnectClient,
    override val preferences: Preferences
) : CodeAuthFlow {
    override suspend fun startLoginFlow(request: AuthCodeRequest) {
        window.location.href = request.url.toString()
    }
}

@OptIn(ExperimentalOpenIdConnect::class)
class WebRedirectEndSessionFlow(
    override val client: OpenIdConnectClient,
    override val preferences: Preferences
) : EndSessionFlow {
    override suspend fun startLogoutFlow(request: EndSessionRequest) {
        window.location.href = request.url.toString()
    }
}