package com.availelabs.availe

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.composeunstyled.Text
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory

@Composable
fun App(root: IRootComponent) {
    Text("Hello, world!")
}

interface IRootComponent {
    val codeAuthFlowFactory: CodeAuthFlowFactory
}

class RootComponent(
    componentContext: ComponentContext,
    override val codeAuthFlowFactory: CodeAuthFlowFactory
) : IRootComponent, ComponentContext by componentContext {
    init {
    }
}
