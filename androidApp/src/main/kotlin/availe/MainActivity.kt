package com.availelabs.availe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.defaultComponentContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val root = RootComponent(componentContext = defaultComponentContext())

        setContent { App(root) }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    class PreviewRootComponent : IRootComponent
    App(PreviewRootComponent())
}