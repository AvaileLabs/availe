package com.availelabs.availe.data.auth

import com.availelabs.availe.core.platform.PlatformActivity
import org.publicvalue.multiplatform.oidc.appsupport.AndroidCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory

actual fun createPlatformAuthenticationFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory {
    val factory = AndroidCodeAuthFlowFactory()
    factory.registerActivity(activity)
    return factory
}