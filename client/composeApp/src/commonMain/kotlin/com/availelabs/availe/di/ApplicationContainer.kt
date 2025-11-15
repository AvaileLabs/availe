package com.availelabs.availe.di

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.data.auth.*
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect

interface ApplicationContainer {
    val authenticationService: IAuthenticationService
}

@OptIn(ExperimentalOpenIdConnect::class)
fun createApplicationContainer(
    lifecycle: Lifecycle,
    activity: PlatformActivity
): ApplicationContainer {
    val authFactory = createPlatformAuthenticationFlowFactory(activity)
    val tokenStore = createTokenStore(activity)
    val platformAuthConfig = createPlatformAuthenticationConfig(activity)

    val authenticationService = AuthenticationService(
        authFactory = authFactory,
        tokenStore = tokenStore,
        platformAuthConfig = platformAuthConfig,
        lifecycle = lifecycle
    )

    return DefaultApplicationContainer(
        authenticationService = authenticationService
    )
}

private class DefaultApplicationContainer(
    override val authenticationService: IAuthenticationService
) : ApplicationContainer