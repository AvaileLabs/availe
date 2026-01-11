package com.availelabs.availe.appshell.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.*
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.availelabs.availe.appshell.component.AppShellChildComponent
import com.availelabs.availe.appshell.component.IAppShellComponent
import com.availelabs.availe.appshell.model.AppShellConfiguration
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.chat.root.ChatRootScreen
import com.availelabs.availe.navigation.DeepLinkRoute
import com.availelabs.availe.ui.components.LinkContextMenuArea
import com.availelabs.availe.ui.components.dialogs.ChatOverlayWindow
import com.availelabs.availe.ui.layout.AppTopNavigation
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.User
import com.composables.uikit.components.Icon
import com.composables.uikit.components.IconButton
import com.composables.uikit.components.PrimaryButton
import com.composables.uikit.components.Text
import com.composables.uikit.styling.accent
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.onBackground
import com.composables.uikit.styling.textStyles
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
    val activeConfig = childStack.active.configuration

    val selectedIndex = when (activeConfig) {
        is AppShellConfiguration.Home -> 0
        is AppShellConfiguration.Dashboard -> 0
        is AppShellConfiguration.Chat -> 1
        is AppShellConfiguration.Profile -> 2
        else -> -1
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopNavigation(
                logo = {
                    Text("Availe", fontWeight = FontWeight.Bold)
                },
                navigationItems = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home
                        LinkContextMenuArea(route = DeepLinkRoute.Home) {
                            IconButton(
                                onClick = { appShellComponent.onHomeTabClicked() },
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
                        }

                        // Chat
                        LinkContextMenuArea(route = DeepLinkRoute.Chat()) {
                            IconButton(
                                onClick = { appShellComponent.onChatTabClicked() },
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
                        }

                        // Profile
                        val profileButton = @Composable {
                            IconButton(
                                onClick = { appShellComponent.onProfileTabClicked() },
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

                        if (authenticationState is AuthenticationState.LoggedIn) {
                            LinkContextMenuArea(route = DeepLinkRoute.Profile(authenticationState.userId)) {
                                profileButton()
                            }
                        } else {
                            profileButton()
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
                                if (authenticationState is AuthenticationState.LoggedIn) {
                                    ChatRootScreen(
                                        component = overlayInstance.component
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Lucide.MessageCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp).background(
                                                Theme[colors][accent].copy(alpha = 0.1f),
                                                CircleShape
                                            ).padding(12.dp)
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        SelectionContainer {
                                            Text(
                                                text = stringResource(Res.string.authentication_guard_title),
                                                style = Theme[textStyles][com.composables.uikit.styling.title],
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        SelectionContainer {
                                            Text(
                                                text = stringResource(Res.string.authentication_guard_body),
                                                style = Theme[textStyles][com.composables.uikit.styling.body].copy(
                                                    color = Theme[colors][onBackground].copy(alpha = 0.7f)
                                                ),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        Spacer(Modifier.height(24.dp))
                                        PrimaryButton(
                                            onClick = { appShellComponent.onLoginClicked() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(Res.string.authentication_log_in))
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}