package com.availelabs.availe.auth

import com.availelabs.availe.platform.PlatformActivity
import org.publicvalue.multiplatform.oidc.appsupport.IosCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory

actual fun createPlatformAuthenticationFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory {
    return IosCodeAuthFlowFactory()
}