package com.availelabs.availe.features.appshell

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.features.chat.ChatScreen
import com.availelabs.availe.features.home.HomeScreen
import com.availelabs.availe.features.home.HomeStore
import com.availelabs.availe.features.listingdetails.ListingDetailsScreen
import com.availelabs.availe.ui.animation.appStackAnimation
import com.availelabs.availe.ui.components.ChatOverlayWindow
import com.availelabs.availe.ui.components.PredictiveBackGestureOverlay
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.LogIn
import com.composables.icons.lucide.LogOut
import com.composables.uikit.components.*

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

    LaunchedEffect(activeConfiguration) {
        focusManager.clearFocus()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 800.dp

        LaunchedEffect(isWideScreen) {
            appShellComponent.setChatOverlayMode(isActive = isWideScreen)
        }

        PredictiveBackGestureOverlay(
            backDispatcher = backDispatcher,
            backIcon = null,
            modifier = Modifier.fillMaxSize(),
            content = {
                AppScaffold {
                    TopAppBar(
                        navigation = {
                            val isBackStackEmpty = childStack.backStack.isEmpty()
                            if (isBackStackEmpty) {
                                IconButton(onClick = { }) {
                                    Icon(
                                        imageVector = activeConfiguration.navigationIcon.icon,
                                        contentDescription = activeConfiguration.navigationIcon.contentDescription
                                    )
                                }
                            } else {
                                IconButton(onClick = { appShellComponent.onBackClicked() }) {
                                    Icon(
                                        imageVector = com.composables.icons.lucide.Lucide.ArrowLeft,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        },
                        title = {
                            Text(activeConfiguration.displayName)
                        },
                        actions = {
                            when (val activeInstance = childStack.active.instance) {
                                is AppShellChildComponent.Home -> {
                                    if (authenticationState is AuthenticationState.LoggedIn) {
                                        IconButton(
                                            onClick = { activeInstance.component.onIntent(HomeStore.Intent.QuickCreateListingRequested) }
                                        ) {
                                            Icon(com.composables.icons.lucide.Lucide.CirclePlus, "Add")
                                        }
                                    }
                                }

                                else -> {}
                            }

                            if (authenticationState is AuthenticationState.LoggedIn) {
                                IconButton(
                                    onClick = { appShellComponent.onLogoutClicked() }
                                ) {
                                    Icon(com.composables.icons.lucide.Lucide.LogOut, "Logout")
                                }
                            } else {
                                if (isAuthenticationLoading) {
                                    DestructiveButton(
                                        onClick = { appShellComponent.onCancelLoginClicked() }
                                    ) {
                                        Text("Cancel")
                                    }
                                } else {
                                    PrimaryButton(
                                        onClick = { appShellComponent.onLoginClicked() }
                                    ) {
                                        if (authenticationState is AuthenticationState.InitializationFailed) {
                                            Icon(com.composables.icons.lucide.Lucide.LogIn, null)
                                            Text("Retry")
                                        } else {
                                            Icon(com.composables.icons.lucide.Lucide.LogIn, null)
                                            Text("Log In")
                                        }
                                    }
                                }
                            }
                        }
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column {
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
                        }

                        val overlayInstance = chatOverlay.child?.instance
                        if (isWideScreen && overlayInstance != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 24.dp, bottom = 24.dp)
                            ) {
                                ChatOverlayWindow(
                                    title = "Messaging",
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
            }
        )
    }
}

@Composable
private fun AppShellChildComponent.Render(modifier: Modifier) {
    when (this) {
        is AppShellChildComponent.Home -> HomeScreen(
            homeComponent = this.component,
            onSearchClick = { },
            onNotificationClick = { }
        )

        is AppShellChildComponent.ListingDetails -> ListingDetailsScreen(
            listingDetailsComponent = this.component
        )

        is AppShellChildComponent.Chat -> ChatScreen(
            component = this.component,
            isOverlay = false
        )
    }
}