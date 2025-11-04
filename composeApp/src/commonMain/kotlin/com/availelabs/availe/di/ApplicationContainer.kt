package com.availelabs.availe.di

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.data.auth.AuthService
import com.availelabs.availe.data.auth.IAuthService
import com.availelabs.availe.data.auth.createPlatformAuthenticationConfig
import com.availelabs.availe.data.auth.createPlatformAuthenticationFlowFactory
import com.availelabs.availe.data.auth.createTokenStore
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect

interface ApplicationContainer {
    val authService: IAuthService
}

@OptIn(ExperimentalOpenIdConnect::class)
fun createApplicationContainer(
    lifecycle: Lifecycle,
    activity: PlatformActivity
): ApplicationContainer {
    val authFactory = createPlatformAuthenticationFlowFactory(activity)
    val tokenStore = createTokenStore(activity)
    val platformAuthConfig = createPlatformAuthenticationConfig(activity)

    val authService = AuthService(
        authFactory = authFactory,
        tokenStore = tokenStore,
        platformAuthConfig = platformAuthConfig,
        lifecycle = lifecycle
    )

    return DefaultApplicationContainer(
        authService = authService
    )
}

private class DefaultApplicationContainer(
    override val authService: IAuthService
) : ApplicationContainer