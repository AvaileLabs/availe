package com.availelabs.availe.data.auth

import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory

expect class PlatformActivity

expect fun createPlatformAuthFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory