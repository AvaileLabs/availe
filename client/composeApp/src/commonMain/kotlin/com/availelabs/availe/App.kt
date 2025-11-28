package com.availelabs.availe

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.features.home.HomeScreen
import com.availelabs.availe.features.root.ChildComponent
import com.availelabs.availe.features.root.Configuration
import com.availelabs.availe.features.root.IRootComponent
import com.composables.uikit.components.AppScaffold
import com.composables.uikit.components.Icon
import com.composables.uikit.components.IconButton
import com.composables.uikit.components.TopAppBar
import com.composables.uikit.styling.UiKitTheme
import com.composeunstyled.Text

@Composable
fun App(root: IRootComponent) {
    val childStack by root.childStack.subscribeAsState()
    val activeConfig = childStack.active.configuration as Configuration
    val focusManager = LocalFocusManager.current

    LaunchedEffect(activeConfig) {
        focusManager.clearFocus()
    }

    UiKitTheme {
        AppScaffold {
            TopAppBar(
                navigation = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(
                            imageVector = activeConfig.navigationIcon.icon,
                            contentDescription = activeConfig.navigationIcon.contentDescription
                        )
                    }
                    Text(activeConfig.displayName)
                }
            )

            Box(Modifier.weight(1f).padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column {
                    Children(
                        stack = root.childStack,
                        modifier = Modifier.fillMaxSize()
                    ) { child ->
                        child.instance.Render(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChildComponent.Render(modifier: Modifier) {
    when (this) {
        is ChildComponent.Home -> HomeScreen(
            homeComponent = this.component,
            onSearchClick = { /* TODO */ },
            onNotificationClick = { /* TODO */ }
        )
    }
}