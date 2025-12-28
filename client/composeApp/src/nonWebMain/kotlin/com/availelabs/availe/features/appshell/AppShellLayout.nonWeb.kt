package com.availelabs.availe.features.appshell

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import availe.client.composeapp.generated.resources.Res
import availe.client.composeapp.generated.resources.generic_back
import availe.client.composeapp.generated.resources.navigation_home
import availe.client.composeapp.generated.resources.navigation_profile
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.ui.components.layout.*
import com.availelabs.availe.ui.components.navigation.ShellNavigationItem
import com.availelabs.availe.ui.components.navigation.ShellRailNavigationItem
import com.availelabs.availe.ui.mapper.NavigationUiMapper
import com.availelabs.availe.ui.model.AppBarAction
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
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
    chatOverlay: ChildSlot<AppShellConfiguration.Chat, AppShellChildComponent.Chat>
) {
    val windowSize = rememberWindowSize()
    val activeInstance = childStack.active.instance
    val childActions by activeInstance.actions.subscribeAsState()
    val activeConfiguration = childStack.active.configuration

    val dashboardComponent = (activeInstance as? AppShellChildComponent.Dashboard)?.component
    val isDashboard = dashboardComponent != null

    // Determine navigation UI visibility and state
    val showBottomBar = isDashboard && windowSize == WindowSizeClass.Compact
    val showSideRail = isDashboard && windowSize != WindowSizeClass.Compact

    // Swipe is only enabled on Compact screens (Phone)
    val isSwipeEnabled = windowSize == WindowSizeClass.Compact

    AdaptiveScaffold(
        appBar = {
            SharedTopAppBar(
                appShellComponent = appShellComponent,
                activeConfiguration = activeConfiguration,
                isBackStackEmpty = childStack.backStack.isEmpty(),
                childActions = childActions,
                authenticationState = authenticationState,
                isAuthenticationLoading = isAuthenticationLoading
            )
        },
        bottomBar = if (showBottomBar && dashboardComponent != null) {
            {
                val pages by dashboardComponent.pages.subscribeAsState()
                val selectedIndex = pages.selectedIndex

                AppBottomNavigation {
                    ShellNavigationItem(
                        selected = selectedIndex == 0,
                        onClick = { dashboardComponent.onHomeTabClicked() },
                        icon = { Icon(Lucide.House, null) },
                        label = { Text(stringResource(Res.string.navigation_home)) },
                        modifier = Modifier.weight(1f)
                    )

                    ShellNavigationItem(
                        selected = selectedIndex == 1,
                        onClick = { dashboardComponent.onProfileTabClicked() },
                        icon = { Icon(Lucide.User, null) },
                        label = { Text(stringResource(Res.string.navigation_profile)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else null,
        sideBar = if (showSideRail && dashboardComponent != null) {
            {
                val pages by dashboardComponent.pages.subscribeAsState()
                val selectedIndex = pages.selectedIndex

                AppNavigationRail {
                    ShellRailNavigationItem(
                        selected = selectedIndex == 0,
                        onClick = { dashboardComponent.onHomeTabClicked() },
                        icon = { Icon(Lucide.House, null) }
                    )

                    ShellRailNavigationItem(
                        selected = selectedIndex == 1,
                        onClick = { dashboardComponent.onProfileTabClicked() },
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
                ChatOverlay(
                    appShellComponent = appShellComponent,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}

@Composable
private fun SharedTopAppBar(
    appShellComponent: IAppShellComponent,
    activeConfiguration: AppShellConfiguration,
    isBackStackEmpty: Boolean,
    childActions: List<AppBarAction>,
    authenticationState: AuthenticationState,
    isAuthenticationLoading: Boolean
) {
    val isDashboard = activeConfiguration is AppShellConfiguration.Dashboard

    val (icon, onIconClick) = if (isBackStackEmpty || isDashboard) {
        // Show Home icon for root page visualization
        NavigationUiMapper.map(activeConfiguration).icon to {}
    } else {
        Lucide.ArrowLeft to { appShellComponent.onBackClicked() }
    }

    val contentDescription = if (isBackStackEmpty || isDashboard) {
        stringResource(Res.string.navigation_home)
    } else {
        stringResource(Res.string.generic_back)
    }

    TopAppBar(
        navigation = {
            if (icon != null) {
                IconButton(onClick = onIconClick) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription
                    )
                }
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