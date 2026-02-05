package com.availelabs.availe.appshell.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import availe.client.designsystem.generated.resources.*
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.availelabs.availe.appshell.component.AppShellChildComponent
import com.availelabs.availe.appshell.component.IAppShellComponent
import com.availelabs.availe.appshell.model.AppShellConfiguration
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.navigation.DeepLinkRoute
import com.availelabs.availe.ui.components.LinkContextMenuArea
import com.availelabs.availe.ui.layout.*
import com.availelabs.availe.ui.mapper.NavigationUiMapper
import com.availelabs.availe.ui.model.AppBarAction
import com.availelabs.availe.ui.navigation.ShellNavigationItem
import com.availelabs.availe.ui.navigation.ShellRailNavigationItem
import com.composables.icons.lucide.*
import com.composables.uikit.components.Icon
import com.composables.uikit.components.IconButton
import com.composables.uikit.components.Text
import com.composables.uikit.components.TopAppBar
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

    val dashboardComponent = childStack.items
        .map { it.instance }
        .filterIsInstance<AppShellChildComponent.Dashboard>()
        .firstOrNull()?.component

    val isSwipeEnabled = windowSize == WindowSizeClass.Compact
    val isBottomBarVisible = dashboardComponent != null && windowSize == WindowSizeClass.Compact

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
        bottomBar = if (isBottomBarVisible) {
            {
                val pages by dashboardComponent.pages.subscribeAsState()
                val selectedIndex = pages.selectedIndex

                AppBottomNavigation {
                    LinkContextMenuArea(route = DeepLinkRoute.Home(), modifier = Modifier.weight(1f)) {
                        ShellNavigationItem(
                            selected = selectedIndex == 0,
                            onClick = { navigateToTab { dashboardComponent.onHomeTabClicked() } },
                            icon = { Icon(Lucide.House, null) },
                            label = { Text(stringResource(Res.string.navigation_home)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    LinkContextMenuArea(route = DeepLinkRoute.Chat(), modifier = Modifier.weight(1f)) {
                        ShellNavigationItem(
                            selected = selectedIndex == 1,
                            onClick = { navigateToTab { dashboardComponent.onChatTabClicked() } },
                            icon = { Icon(Lucide.MessageCircle, null) },
                            label = { Text(stringResource(Res.string.navigation_chat)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    val profileItem = @Composable {
                        ShellNavigationItem(
                            selected = selectedIndex == 2,
                            onClick = { navigateToTab { dashboardComponent.onProfileTabClicked() } },
                            icon = { Icon(Lucide.User, null) },
                            label = { Text(stringResource(Res.string.navigation_profile)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (authenticationState is AuthenticationState.LoggedIn) {
                        LinkContextMenuArea(
                            route = DeepLinkRoute.Profile(authenticationState.userId),
                            modifier = Modifier.weight(1f)
                        ) {
                            profileItem()
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            profileItem()
                        }
                    }
                }
            }
        } else null,
        sideBar = if (dashboardComponent != null && windowSize != WindowSizeClass.Compact) {
            {
                val pages by dashboardComponent.pages.subscribeAsState()
                val selectedIndex = pages.selectedIndex

                AppNavigationRail {
                    LinkContextMenuArea(route = DeepLinkRoute.Home()) {
                        ShellRailNavigationItem(
                            selected = selectedIndex == 0,
                            onClick = { navigateToTab { dashboardComponent.onHomeTabClicked() } },
                            icon = { Icon(Lucide.House, null) }
                        )
                    }

                    LinkContextMenuArea(route = DeepLinkRoute.Chat()) {
                        ShellRailNavigationItem(
                            selected = selectedIndex == 1,
                            onClick = { navigateToTab { dashboardComponent.onChatTabClicked() } },
                            icon = { Icon(Lucide.MessageCircle, null) }
                        )
                    }

                    val profileRailItem = @Composable {
                        ShellRailNavigationItem(
                            selected = selectedIndex == 2,
                            onClick = { navigateToTab { dashboardComponent.onProfileTabClicked() } },
                            icon = { Icon(Lucide.User, null) }
                        )
                    }

                    if (authenticationState is AuthenticationState.LoggedIn) {
                        LinkContextMenuArea(route = DeepLinkRoute.Profile(authenticationState.userId)) {
                            profileRailItem()
                        }
                    } else {
                        profileRailItem()
                    }
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
                val contentModifier = if (isBottomBarVisible) {
                    Modifier.consumeAppBottomNavigationInsets()
                } else {
                    Modifier
                }

                AppShellContentStack(
                    appShellComponent = appShellComponent,
                    isSwipeEnabled = isSwipeEnabled,
                    modifier = contentModifier
                )
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
                Text(
                    text = stringResource(Res.string.application_name),
                    fontWeight = FontWeight.Bold
                )
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