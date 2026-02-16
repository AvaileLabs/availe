package com.availelabs.availe.auth

import com.availelabs.availe.platform.PlatformActivity
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.appsupport.JvmCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.webserver.SimpleKtorWebserver
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory

@OptIn(ExperimentalOpenIdConnect::class)
actual fun createPlatformAuthenticationFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory {
    return JvmCodeAuthFlowFactory(
        webserverProvider = {
            SimpleKtorWebserver(port = 8888)
        }
    )
}