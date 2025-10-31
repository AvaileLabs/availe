package com.availelabs.availe

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.composeunstyled.Text
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun App(root: IRootComponent) {
    Text("Hello, world!")
}

@Preview
@Composable
internal fun AppPreview() {
    class PreviewRootComponent : IRootComponent
    App(PreviewRootComponent())
}

interface IRootComponent

class RootComponent(
    componentContext: ComponentContext
) : IRootComponent, ComponentContext by componentContext {
    init {
    }
}
