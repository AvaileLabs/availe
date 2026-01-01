package com.availelabs.availe.features.chat.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.availelabs.availe.features.chat.details.ChatScreen
import com.availelabs.availe.features.chat.list.ChatListScreen
import com.availelabs.availe.ui.components.layout.WindowSizeClass
import com.availelabs.availe.ui.components.layout.rememberWindowSize
import com.composables.uikit.components.Text
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.outline
import com.composeunstyled.theme.Theme

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun ChatRootScreen(
    component: IChatRootComponent,
    modifier: Modifier = Modifier
) {
    val windowSize = rememberWindowSize()
    val panels by component.panels.subscribeAsState()

    // Responsive Logic: Switch between Single and Dual mode based on screen size
    LaunchedEffect(windowSize) {
        val mode = when (windowSize) {
            WindowSizeClass.Compact -> ChildPanelsMode.SINGLE
            else -> ChildPanelsMode.DUAL
        }
        component.setMode(mode)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (windowSize == WindowSizeClass.Compact) {
            // Mobile: Show either Details (if active) OR Main
            // In Single mode, Decompose ensures only one is Resumed/Active.
            val detailsChild = panels.details
            if (detailsChild != null) {
                ChatScreen(component = detailsChild.instance)
            } else {
                ChatListScreen(component = panels.main.instance)
            }
        } else {
            // Desktop/Tablet: Split View
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                ) {
                    ChatListScreen(component = panels.main.instance)

                    // Vertical divider
                    Box(
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.CenterEnd)
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Theme[colors][outline].copy(alpha = 0.2f))
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val detailsChild = panels.details
                    if (detailsChild != null) {
                        ChatScreen(component = detailsChild.instance)
                    } else {
                        // Empty state when no chat selected on desktop
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = "Select a conversation to start messaging",
                                color = Theme[colors][outline]
                            )
                        }
                    }
                }
            }
        }
    }
}