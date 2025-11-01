package com.availelabs.availe.auth

import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory

expect class PlatformActivity

expect fun createPlatformAuthFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory