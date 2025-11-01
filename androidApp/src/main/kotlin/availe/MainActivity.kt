package com.availelabs.availe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.defaultComponentContext
import com.availelabs.availe.auth.createPlatformAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.AndroidCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory

class MainActivity : ComponentActivity() {
    private val codeAuthFlowFactory = AndroidCodeAuthFlowFactory()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val authFactory = createPlatformAuthFlowFactory(this)

        val root = RootComponent(componentContext = defaultComponentContext(), codeAuthFlowFactory = authFactory)
        setContent { App(root) }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    class PreviewRootComponent(override val codeAuthFlowFactory: CodeAuthFlowFactory) : IRootComponent
    App(PreviewRootComponent(
        codeAuthFlowFactory =
            get() =
    ))
}