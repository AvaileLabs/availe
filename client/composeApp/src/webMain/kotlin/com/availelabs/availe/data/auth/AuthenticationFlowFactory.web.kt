@file:OptIn(ExperimentalWasmJsInterop::class)

package com.availelabs.availe.data.auth

import com.availelabs.availe.core.platform.PlatformActivity
import kotlinx.browser.window
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.PlatformCodeAuthFlow
import org.publicvalue.multiplatform.oidc.appsupport.WebCodeAuthFlowFactory
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalOpenIdConnect::class)
actual fun createPlatformAuthenticationFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory {
    return WebCodeAuthFlowFactory()
}

@OptIn(ExperimentalOpenIdConnect::class)
fun handleRedirect(): Boolean {
    return try {
        PlatformCodeAuthFlow.handleRedirect()
        window.opener != null
    } catch (_: Exception) {
        false
    }
}