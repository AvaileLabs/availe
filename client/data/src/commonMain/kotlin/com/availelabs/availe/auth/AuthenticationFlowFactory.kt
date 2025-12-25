package com.availelabs.availe.auth

import com.availelabs.availe.platform.PlatformActivity
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory

expect fun createPlatformAuthenticationFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory