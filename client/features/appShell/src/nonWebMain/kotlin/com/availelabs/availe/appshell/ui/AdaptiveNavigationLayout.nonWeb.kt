package com.availelabs.availe.appshell.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.availelabs.availe.ui.components.dialogs.ChatOverlayWindow
import com.availelabs.availe.ui.layout.*
import com.availelabs.availe.ui.mapper.NavigationUiMapper
import com.availelabs.availe.ui.model.AppBarAction
import com.availelabs.availe.ui.navigation.ShellNavigationItem
import com.availelabs.availe.ui.navigation.ShellRailNavigationItem
import com.composables.icons.lucide.*
import com.composables.uikit.components.*
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
    val windowSize = rememberWindowSize()
    val activeInstance = childStack.active.instance
    val childActions by activeInstance.actions.subscribeAsState()
    val activeConfiguration = childStack.active.configuration
    val showBackArrow by appShellComponent.showBackArrow.subscribeAsState()
    val shouldShowChatOverlay by appShellComponent.shouldShowChatOverlay.subscribeAsState()

    val dashboardComponent = childStack.items
        .map { it.instance }
        .filterIsInstance<AppShellChildComponent.Dashboard>()
        .firstOrNull()?.component

    val isSwipeEnabled = windowSize == WindowSizeClass.Compact

    val navigateToTab: (() -> Unit) -> Unit = { performTabAction ->
        if (activeInstance !is AppShellChildComponent.Dashboard) {
            appShellComponent.onBackClicked()
        }
        performTabAction()
    }

    AdaptiveScaffold(
        appBar = {
            SharedTopAppBar(
                appShellComponent = appShellComponent,
                activeConfiguration = activeConfiguration,
                showBackArrow = showBackArrow,
                childActions = childActions,
                authenticationState = authenticationState,
                isAuthenticationLoading = isAuthenticationLoading
            )
        },
        bottomBar = if (dashboardComponent != null && windowSize == WindowSizeClass.Compact) {
            {
                val pages by dashboardComponent.pages.subscribeAsState()
                val selectedIndex = pages.selectedIndex

                AppBottomNavigation {
                    ShellNavigationItem(
                        selected = selectedIndex == 0,
                        onClick = { navigateToTab { dashboardComponent.onHomeTabClicked() } },
                        icon = { Icon(Lucide.House, null) },
                        label = { Text(stringResource(Res.string.navigation_home)) },
                        modifier = Modifier.weight(1f)
                    )

                    ShellNavigationItem(
                        selected = selectedIndex == 1,
                        onClick = { navigateToTab { dashboardComponent.onChatTabClicked() } },
                        icon = { Icon(Lucide.MessageCircle, null) },
                        label = { Text(stringResource(Res.string.navigation_chat)) },
                        modifier = Modifier.weight(1f)
                    )

                    ShellNavigationItem(
                        selected = selectedIndex == 2 || (selectedIndex == 1 && activeConfiguration is AppShellConfiguration.Dashboard && pages.items.size < 3),
                        onClick = { navigateToTab { dashboardComponent.onProfileTabClicked() } },
                        icon = { Icon(Lucide.User, null) },
                        label = { Text(stringResource(Res.string.navigation_profile)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else null,
        sideBar = if (dashboardComponent != null && windowSize != WindowSizeClass.Compact) {
            {
                val pages by dashboardComponent.pages.subscribeAsState()
                val selectedIndex = pages.selectedIndex

                AppNavigationRail {
                    ShellRailNavigationItem(
                        selected = selectedIndex == 0,
                        onClick = { navigateToTab { dashboardComponent.onHomeTabClicked() } },
                        icon = { Icon(Lucide.House, null) }
                    )

                    ShellRailNavigationItem(
                        selected = selectedIndex == 1,
                        onClick = { navigateToTab { dashboardComponent.onChatTabClicked() } },
                        icon = { Icon(Lucide.MessageCircle, null) }
                    )

                    ShellRailNavigationItem(
                        selected = selectedIndex == 2,
                        onClick = { navigateToTab { dashboardComponent.onProfileTabClicked() } },
                        icon = { Icon(Lucide.User, null) }
                    )
                }
            }
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                AppShellContentStack(
                    appShellComponent = appShellComponent,
                    isSwipeEnabled = isSwipeEnabled
                )

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
                                        Text(
                                            text = stringResource(Res.string.authentication_guard_title),
                                            style = Theme[textStyles][com.composables.uikit.styling.title],
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(Res.string.authentication_guard_body),
                                            style = Theme[textStyles][com.composables.uikit.styling.body].copy(
                                                color = Theme[colors][onBackground].copy(alpha = 0.7f)
                                            ),
                                            textAlign = TextAlign.Center
                                        )
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

@Composable
private fun SharedTopAppBar(
    appShellComponent: IAppShellComponent,
    activeConfiguration: AppShellConfiguration,
    showBackArrow: Boolean,
    childActions: List<AppBarAction>,
    authenticationState: AuthenticationState,
    isAuthenticationLoading: Boolean
) {
    val isDashboard = activeConfiguration is AppShellConfiguration.Dashboard

    val (icon, onIconClick) = if (showBackArrow) {
        Lucide.ArrowLeft to { appShellComponent.onBackClicked() }
    } else {
        NavigationUiMapper.map(activeConfiguration).icon to {}
    }

    val contentDescription = if (showBackArrow) {
        stringResource(Res.string.generic_back)
    } else {
        stringResource(Res.string.navigation_home)
    }

    TopAppBar(
        navigation = {
            IconButton(onClick = onIconClick) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription
                )
            }
        },
        title = {
            if (isDashboard) {
                Text("Availe", fontWeight = FontWeight.Bold)
            } else {
                val navState = NavigationUiMapper.map(activeConfiguration)
                Text(stringResource(navState.title))
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
}