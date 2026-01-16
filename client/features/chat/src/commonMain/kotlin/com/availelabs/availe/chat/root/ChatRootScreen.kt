package com.availelabs.availe.chat.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.chat_empty_details_state
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.availelabs.availe.chat.details.ChatScreen
import com.availelabs.availe.chat.list.ChatListScreen
import com.availelabs.availe.ui.layout.WindowSizeClass
import com.availelabs.availe.ui.layout.rememberWindowSize
import com.composables.uikit.components.Text
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.outline
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

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
                            .align(Alignment.CenterEnd)
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
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.chat_empty_details_state),
                                color = Theme[colors][outline]
                            )
                        }
                    }
                }
            }
        }
    }
}