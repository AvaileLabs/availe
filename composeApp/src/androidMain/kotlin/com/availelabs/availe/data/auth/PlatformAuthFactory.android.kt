package com.availelabs.availe.data.auth

import androidx.activity.ComponentActivity
import org.publicvalue.multiplatform.oidc.appsupport.AndroidCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory

actual typealias PlatformActivity = ComponentActivity

actual fun createPlatformAuthFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory {
    val factory = AndroidCodeAuthFlowFactory()
    factory.registerActivity(activity)
    return factory
}