package com.availelabs.availe.di

import IAppContainer
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.availelabs.availe.data.auth.*
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect

@OptIn(ExperimentalOpenIdConnect::class)
fun createAppContainer(
    lifecycle: Lifecycle,
    activity: PlatformActivity
): IAppContainer {
    val authFactory = createPlatformAuthFlowFactory(activity)
    val tokenStore = createTokenStore(activity)
    val platformAuthConfig = createPlatformAuthConfig(activity)

    val authService = AuthService(
        authFactory = authFactory,
        tokenStore = tokenStore,
        platformAuthConfig = platformAuthConfig,
        lifecycle = lifecycle
    )

    return AppContainerImpl(
        authService = authService
    )
}

private class AppContainerImpl(
    override val authService: IAuthService
) : IAppContainer