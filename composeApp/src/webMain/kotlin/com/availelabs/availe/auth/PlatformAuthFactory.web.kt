package com.availelabs.availe.auth

import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.WebMainCodeAuthFlowFactory

actual class PlatformActivity

@OptIn(ExperimentalOpenIdConnect::class)
actual fun createPlatformAuthFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory {
    return WebMainCodeAuthFlowFactory()
}