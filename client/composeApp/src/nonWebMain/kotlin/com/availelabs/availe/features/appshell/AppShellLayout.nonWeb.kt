package com.availelabs.availe.features.appshell

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import availe.client.composeapp.generated.resources.Res
import availe.client.composeapp.generated.resources.accessibility_action_logout
import availe.client.composeapp.generated.resources.generic_back
import availe.client.composeapp.generated.resources.navigation_home
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.ui.components.*
import com.availelabs.availe.ui.mapper.ActionIconMapper
import com.availelabs.availe.ui.model.asString
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.House
import com.composables.icons.lucide.LogOut
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
            TopAppBar(
                navigation = {
                    val isBackStackEmpty = childStack.backStack.isEmpty()
                    if (isBackStackEmpty) {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = activeConfiguration.navigationIcon.icon,
                                contentDescription = stringResource(activeConfiguration.navigationIcon.contentDescription)
                            )
                        }
                    } else {
                        IconButton(onClick = { appShellComponent.onBackClicked() }) {
                            Icon(
                                imageVector = Lucide.ArrowLeft,
                                contentDescription = stringResource(Res.string.generic_back)
                            )
                        }
                    }
                },
                title = {
                    if (activeConfiguration is AppShellConfiguration.Home) {
                        Text("Availe", fontWeight = FontWeight.Bold)
                    } else {
                        Text(stringResource(activeConfiguration.titleResource))
                    }
                },
                actions = {
                    childActions.forEach { action ->
                        val iconVector = ActionIconMapper.map(action.icon)
                        IconButton(onClick = action.onClick) {
                            Icon(iconVector, action.label.asString())
                        }
                    }

                    if (authenticationState is AuthenticationState.LoggedIn) {
                        IconButton(
                            onClick = { appShellComponent.onLogoutClicked() }
                        ) {
                            Icon(Lucide.LogOut, stringResource(Res.string.accessibility_action_logout))
                        }
                    } else {
                        AuthenticationActionButton(
                            isLoading = isAuthenticationLoading,
                            onLoginClicked = { appShellComponent.onLoginClicked() },
                            onCancelLoginClicked = { appShellComponent.onCancelLoginClicked() }
                        )
                    }
                }
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
            TopAppBar(
                navigation = {
                    val isBackStackEmpty = childStack.backStack.isEmpty()
                    if (isBackStackEmpty) {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = activeConfiguration.navigationIcon.icon,
                                contentDescription = stringResource(activeConfiguration.navigationIcon.contentDescription)
                            )
                        }
                    } else {
                        IconButton(onClick = { appShellComponent.onBackClicked() }) {
                            Icon(
                                imageVector = Lucide.ArrowLeft,
                                contentDescription = stringResource(Res.string.generic_back)
                            )
                        }
                    }
                },
                title = {
                    if (activeConfiguration is AppShellConfiguration.Home) {
                        Text("Availe", fontWeight = FontWeight.Bold)
                    } else {
                        Text(stringResource(activeConfiguration.titleResource))
                    }
                },
                actions = {
                    childActions.forEach { action ->
                        val iconVector = ActionIconMapper.map(action.icon)
                        IconButton(onClick = action.onClick) {
                            Icon(iconVector, action.label.asString())
                        }
                    }

                    if (authenticationState is AuthenticationState.LoggedIn) {
                        IconButton(
                            onClick = { appShellComponent.onLogoutClicked() }
                        ) {
                            Icon(Lucide.LogOut, stringResource(Res.string.accessibility_action_logout))
                        }
                    } else {
                        AuthenticationActionButton(
                            isLoading = isAuthenticationLoading,
                            onLoginClicked = { appShellComponent.onLoginClicked() },
                            onCancelLoginClicked = { appShellComponent.onCancelLoginClicked() }
                        )
                    }
                }
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
                AppShellChatOverlay(
                    appShellComponent = appShellComponent,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}