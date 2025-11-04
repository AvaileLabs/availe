package com.availelabs.availe.data.auth

import com.availelabs.availe.core.platform.PlatformActivity
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.JvmCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.webserver.SimpleKtorWebserver

@OptIn(ExperimentalOpenIdConnect::class)
actual fun createPlatformAuthenticationFlowFactory(activity: PlatformActivity): CodeAuthFlowFactory {
    return JvmCodeAuthFlowFactory(
        webserverProvider = {
            SimpleKtorWebserver(port = 8888)
        }
    )
}