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
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.ui.components.layout.*
import com.availelabs.availe.ui.components.navigation.ShellNavigationItem
import com.availelabs.availe.ui.components.navigation.ShellRailNavigationItem
import com.availelabs.availe.ui.mapper.NavigationUiMapper
import com.availelabs.availe.ui.model.AppBarAction
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
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

    when (windowSize) {
        WindowSizeClass.Compact -> {
            CompactShell(
                appShellComponent = appShellComponent,
                authenticationState = authenticationState,
                isAuthenticationLoading = isAuthenticationLoading
            )
        }

        else -> {
            NonCompactShell(
                appShellComponent = appShellComponent,
                authenticationState = authenticationState,
                isAuthenticationLoading = isAuthenticationLoading
            )
        }
    }
}

@Composable
private fun CompactShell(
    appShellComponent: IAppShellComponent,
    authenticationState: AuthenticationState,
    isAuthenticationLoading: Boolean
) {
    val childStack by appShellComponent.childStack.subscribeAsState()
    val activeInstance = childStack.active.instance
    val childActions by activeInstance.actions.subscribeAsState()
    val activeConfiguration = childStack.active.configuration

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
        bottomBar = {
            AppBottomNavigation {
                val isHomeSelected =
                    activeConfiguration is AppShellConfiguration.Home || activeConfiguration.canonicalParent is AppShellConfiguration.Home
                ShellNavigationItem(
                    selected = isHomeSelected,
                    onClick = { appShellComponent.onHomeTabClicked() },
                    icon = { Icon(Lucide.House, null) },
                    label = { Text(stringResource(Res.string.navigation_home)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) {
        AppShellContentStack(appShellComponent)
    }
}

@Composable
private fun NonCompactShell(
    appShellComponent: IAppShellComponent,
    authenticationState: AuthenticationState,
    isAuthenticationLoading: Boolean
) {
    val childStack by appShellComponent.childStack.subscribeAsState()
    val activeInstance = childStack.active.instance
    val childActions by activeInstance.actions.subscribeAsState()
    val activeConfiguration = childStack.active.configuration

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
        sideBar = {
            AppNavigationRail(
                navigationItems = {
                    val isHomeSelected =
                        activeConfiguration is AppShellConfiguration.Home || activeConfiguration.canonicalParent is AppShellConfiguration.Home
                    ShellRailNavigationItem(
                        selected = isHomeSelected,
                        onClick = { appShellComponent.onHomeTabClicked() },
                        icon = { Icon(Lucide.House, null) }
                    )
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                AppShellContentStack(appShellComponent)
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
    val navigationUiState = NavigationUiMapper.map(activeConfiguration)

    val (icon, onIconClick) = if (isBackStackEmpty) {
        navigationUiState.icon to {}
    } else {
        Lucide.ArrowLeft to { appShellComponent.onBackClicked() }
    }

    val contentDescription = if (isBackStackEmpty) {
        stringResource(navigationUiState.contentDescription)
    } else {
        stringResource(Res.string.generic_back)
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
            if (activeConfiguration is AppShellConfiguration.Home) {
                Text("Availe", fontWeight = FontWeight.Bold)
            } else {
                Text(stringResource(navigationUiState.title))
            }
        },
        actions = {
            ShellActions(
                actions = childActions,
                authenticationState = authenticationState,
                isAuthenticationLoading = isAuthenticationLoading,
                onLoginClicked = { appShellComponent.onLoginClicked() },
                onSettingsClick = { appShellComponent.onProfileClicked() },
                onCancelLoginClicked = { appShellComponent.onCancelLoginClicked() }
            )
        }
    )
}