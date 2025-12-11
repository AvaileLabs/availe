package com.availelabs.availe.features.appshell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import availe.client.composeapp.generated.resources.*
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.features.chat.ChatScreen
import com.availelabs.availe.features.home.HomeStore
import com.availelabs.availe.ui.animation.appStackAnimation
import com.availelabs.availe.ui.components.*
import com.composables.icons.lucide.*
import com.composables.uikit.components.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun AppShellScreen(
    appShellComponent: IAppShellComponent
) {
    val childStack by appShellComponent.childStack.subscribeAsState()
    val chatOverlay by appShellComponent.chatOverlay.subscribeAsState()
    val authenticationState by appShellComponent.authenticationState.subscribeAsState()
    val isAuthenticationLoading by appShellComponent.isAuthenticationLoading.subscribeAsState()
    val activeConfiguration = childStack.active.configuration
    val focusManager = LocalFocusManager.current
    val backDispatcher = appShellComponent.backDispatcher
    val windowSize = rememberWindowSize()

    LaunchedEffect(activeConfiguration) {
        focusManager.clearFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PredictiveBackGestureOverlay(
            backDispatcher = backDispatcher,
            backIcon = null,
            modifier = Modifier.fillMaxSize(),
            content = {
                AvaileScaffold(
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
                                when (val activeInstance = childStack.active.instance) {
                                    is AppShellChildComponent.Home -> {
                                        if (authenticationState is AuthenticationState.LoggedIn) {
                                            IconButton(
                                                onClick = { activeInstance.component.onIntent(HomeStore.Intent.QuickCreateListingRequested) }
                                            ) {
                                                Icon(
                                                    Lucide.CirclePlus,
                                                    stringResource(Res.string.accessibility_action_add)
                                                )
                                            }
                                        }
                                    }

                                    else -> {}
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
                        SideBar(
                            navigationItems = {
                                val isHomeSelected =
                                    activeConfiguration is AppShellConfiguration.Home || activeConfiguration.canonicalParent is AppShellConfiguration.Home
                                SideBarItem(
                                    selected = isHomeSelected,
                                    onClick = { /* Navigate Home Logic */ },
                                    icon = { Icon(Lucide.House, null) },
                                    title = { Text(stringResource(Res.string.navigation_home)) }
                                )
                            }
                        )
                    },
                    bottomBar = {
                        BottomNavigationBar {
                            val isHomeSelected =
                                activeConfiguration is AppShellConfiguration.Home || activeConfiguration.canonicalParent is AppShellConfiguration.Home
                            PrimaryTab(
                                selected = isHomeSelected,
                                onSelected = { /* Navigate Home Logic */ },
                                icon = { Icon(Lucide.House, null) },
                                title = { Text(stringResource(Res.string.navigation_home)) }
                            )
                        }
                    }
                ) {
                    Children(
                        stack = childStack,
                        modifier = Modifier.fillMaxSize(),
                        animation = appStackAnimation(
                            backHandler = appShellComponent.backHandler,
                            onBack = { appShellComponent.onBackClicked() }
                        )
                    ) { child ->
                        child.instance.Render(
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    val isOverlayVisible = windowSize is WindowSize.Expanded
                    LaunchedEffect(isOverlayVisible) {
                        appShellComponent.setChatOverlayMode(isActive = isOverlayVisible)
                    }

                    val overlayInstance = chatOverlay.child?.instance
                    if (isOverlayVisible && overlayInstance != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 24.dp, bottom = 24.dp)
                        ) {
                            ChatOverlayWindow(
                                title = stringResource(Res.string.chat_title),
                                onExpand = appShellComponent::expandChatOverlay,
                                onClose = appShellComponent::dismissChatOverlay
                            ) {
                                ChatScreen(
                                    component = overlayInstance.component,
                                    isOverlay = true
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun AppShellChildComponent.Render(modifier: Modifier) {
    when (this) {
        is AppShellChildComponent.Home -> com.availelabs.availe.features.home.HomeScreen(
            homeComponent = this.component,
            onSearchClick = { },
            onNotificationClick = { }
        )

        is AppShellChildComponent.ListingDetails -> com.availelabs.availe.features.listingdetails.ListingDetailsScreen(
            listingDetailsComponent = this.component
        )

        is AppShellChildComponent.Chat -> ChatScreen(
            component = this.component,
            isOverlay = false
        )
    }
}