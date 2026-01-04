package com.availelabs.availe.appshell

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.*
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.chat.root.ChatRootScreen
import com.availelabs.availe.ui.components.dialogs.ChatOverlayWindow
import com.availelabs.availe.ui.layout.AppTopNavigation
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.User
import com.composables.uikit.components.Icon
import com.composables.uikit.components.IconButton
import com.composables.uikit.components.Text
import com.composables.uikit.styling.accent
import com.composables.uikit.styling.colors
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun AppShellLayout(
    appShellComponent: IAppShellComponent,
    childStack: ChildStack<AppShellConfiguration, AppShellChildComponent>,
    authenticationState: AuthenticationState,
    isAuthenticationLoading: Boolean,
    chatOverlay: ChildSlot<AppShellConfiguration.ChatOverlay, AppShellChildComponent.ChatOverlay>
) {
    val activeInstance = childStack.active.instance
    val childActions by activeInstance.actions.subscribeAsState()
    val shouldShowChatOverlay by appShellComponent.shouldShowChatOverlay.subscribeAsState()

    val dashboardComponent = childStack.items
        .map { it.instance }
        .filterIsInstance<AppShellChildComponent.Dashboard>()
        .firstOrNull()?.component

    val navigateToTab: (() -> Unit) -> Unit = { performTabAction ->
        if (activeInstance !is AppShellChildComponent.Dashboard) {
            appShellComponent.onBackClicked()
        }
        performTabAction()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopNavigation(
                logo = {
                    Text("Availe", fontWeight = FontWeight.Bold)
                },
                navigationItems = {
                    if (dashboardComponent != null) {
                        val pages by dashboardComponent.pages.subscribeAsState()
                        val selectedIndex = pages.selectedIndex

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Home
                            IconButton(
                                onClick = { navigateToTab { dashboardComponent.onHomeTabClicked() } },
                                backgroundColor = if (selectedIndex == 0) Theme[colors][accent].copy(alpha = 0.1f) else Color.Transparent
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Lucide.House, null)
                                    Text(stringResource(Res.string.navigation_home))
                                }
                            }

                            // Chat
                            IconButton(
                                onClick = { navigateToTab { dashboardComponent.onChatTabClicked() } },
                                backgroundColor = if (selectedIndex == 1) Theme[colors][accent].copy(alpha = 0.1f) else Color.Transparent
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Lucide.MessageCircle, null)
                                    Text(stringResource(Res.string.navigation_chat))
                                }
                            }

                            // Profile
                            if (authenticationState is AuthenticationState.LoggedIn) {
                                IconButton(
                                    onClick = { navigateToTab { dashboardComponent.onProfileTabClicked() } },
                                    backgroundColor = if (selectedIndex == 2) Theme[colors][accent].copy(alpha = 0.1f) else Color.Transparent
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Lucide.User, null)
                                        Text(stringResource(Res.string.navigation_profile))
                                    }
                                }
                            }
                        }
                    }
                },
                actions = {
                    ShellActions(
                        actions = childActions,
                        authenticationState = authenticationState,
                        isAuthenticationLoading = isAuthenticationLoading,
                        onLoginClicked = { appShellComponent.onLoginClicked() },
                        onSettingsClick = { appShellComponent.onSettingsClicked() },
                        onCancelLoginClicked = { appShellComponent.onCancelLoginClicked() }
                    )
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1200.dp)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                ) {
                    AppShellContentStack(
                        appShellComponent = appShellComponent,
                        isSwipeEnabled = false
                    )
                }

                if (shouldShowChatOverlay && chatOverlay.child != null) {
                    val overlayInstance = chatOverlay.child!!.instance
                    // Use a Box to align bottom-end
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 24.dp)
                    ) {
                        ChatOverlayWindow(
                            title = stringResource(Res.string.chat_title),
                            onExpand = appShellComponent::expandChatOverlay,
                            onClose = appShellComponent::dismissChatOverlay,
                            content = {
                                ChatRootScreen(
                                    component = overlayInstance.component
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}