package com.availelabs.availe.data.auth

import kotlinx.browser.window
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlow
import org.publicvalue.multiplatform.oidc.flows.EndSessionFlow
import org.publicvalue.multiplatform.oidc.preferences.Preferences
import org.publicvalue.multiplatform.oidc.types.AuthCodeRequest
import org.publicvalue.multiplatform.oidc.types.EndSessionRequest

@OptIn(ExperimentalOpenIdConnect::class)
class WebRedirectCodeAuthFlowFactory : CodeAuthFlowFactory {
    override fun createAuthFlow(client: OpenIdConnectClient): CodeAuthFlow {
        return WebRedirectCodeAuthFlow(client, WebLocalStoragePreferences())
    }

    override fun createEndSessionFlow(client: OpenIdConnectClient): EndSessionFlow {
        return WebRedirectEndSessionFlow(client, WebLocalStoragePreferences())
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

private class WebLocalStoragePreferences : Preferences {
    override suspend fun get(key: String): String? = window.localStorage.getItem(key)
    override suspend fun put(key: String, value: String) = window.localStorage.setItem(key, value)
    override suspend fun remove(key: String) = window.localStorage.removeItem(key)
    override suspend fun clear() = window.localStorage.clear()
}