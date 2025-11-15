package com.availelabs.availe.data.auth

import com.availelabs.availe.core.platform.PlatformActivity
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.IosCodeAuthFlowFactory

actual fun createPlatformAuthenticationFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory {
    return IosCodeAuthFlowFactory()
}