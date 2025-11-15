package com.availelabs.availe.data.auth

import com.availelabs.availe.core.platform.PlatformActivity
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory

expect fun createPlatformAuthenticationFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory