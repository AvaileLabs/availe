@file:OptIn(ExperimentalWasmJsInterop::class)

package com.availelabs.availe.auth

import kotlinx.browser.window
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.PlatformCodeAuthFlow
import org.publicvalue.multiplatform.oidc.appsupport.WebMainCodeAuthFlowFactory
import kotlin.js.ExperimentalWasmJsInterop

actual class PlatformActivity

@OptIn(ExperimentalOpenIdConnect::class)
actual fun createPlatformAuthFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory {
    return WebMainCodeAuthFlowFactory()
}

@OptIn(ExperimentalOpenIdConnect::class)
fun handleRedirect(): Boolean {
    return try {
        PlatformCodeAuthFlow.handleRedirect()
        window.opener != null
    } catch (e: Exception) {
        false
    }
}