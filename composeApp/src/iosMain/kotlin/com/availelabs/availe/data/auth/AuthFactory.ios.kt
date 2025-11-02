package com.availelabs.availe.data.auth

import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.IosCodeAuthFlowFactory

actual class PlatformActivity

actual fun createPlatformAuthFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory {
    return IosCodeAuthFlowFactory()
}