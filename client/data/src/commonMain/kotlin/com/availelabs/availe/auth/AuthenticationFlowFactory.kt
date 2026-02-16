package com.availelabs.availe.auth

import com.availelabs.availe.platform.PlatformActivity
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory

internal expect fun createPlatformAuthenticationFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory