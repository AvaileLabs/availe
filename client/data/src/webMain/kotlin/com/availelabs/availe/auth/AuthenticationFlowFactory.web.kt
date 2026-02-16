@file:OptIn(ExperimentalWasmJsInterop::class)

package com.availelabs.availe.auth

import com.availelabs.availe.platform.PlatformActivity
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalOpenIdConnect::class)
actual fun createPlatformAuthenticationFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory {
    return WebRedirectCodeAuthFlowFactory()
}