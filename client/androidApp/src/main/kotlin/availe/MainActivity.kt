package com.availelabs.availe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.availelabs.availe.di.createApplicationContainer
import com.availelabs.availe.features.root.RootComponent
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalOpenIdConnect::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val componentContext = defaultComponentContext()

        val appContainer = createApplicationContainer(componentContext.lifecycle, this)

        val root = RootComponent(
            componentContext = componentContext,
            appContainer = appContainer
        )
        setContent { App(root) }
    }
}