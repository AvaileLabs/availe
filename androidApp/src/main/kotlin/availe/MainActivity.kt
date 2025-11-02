package com.availelabs.availe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.availelabs.availe.data.auth.createPlatformAuthConfig
import com.availelabs.availe.data.auth.createPlatformAuthFlowFactory
import com.availelabs.availe.data.auth.createTokenStore
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalOpenIdConnect::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val authFactory = createPlatformAuthFlowFactory(this)
        val tokenStore = createTokenStore(this)
        val platformAuthConfig = createPlatformAuthConfig(this)

        val root = RootComponent(
            componentContext = defaultComponentContext(),
            codeAuthFlowFactory = authFactory,
            tokenStore = tokenStore,
            platformAuthConfig = platformAuthConfig
        )
        setContent { App(root) }
    }
}