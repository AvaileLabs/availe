package com.availelabs.availe

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.features.home.HomeScreen
import com.availelabs.availe.features.home.HomeStore
import com.availelabs.availe.features.home.IHomeComponent
import com.availelabs.availe.features.listingdetails.ListingDetailsScreen
import com.availelabs.availe.features.main.MainStore
import com.availelabs.availe.features.root.ChildComponent
import com.availelabs.availe.features.root.Configuration
import com.availelabs.availe.features.root.IRootComponent
import com.availelabs.availe.ui.components.PredictiveBackGestureOverlay
import com.composables.icons.lucide.*
import com.composables.uikit.components.*
import com.composables.uikit.styling.UiKitTheme

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun App(rootComponent: IRootComponent) {
    val childComponentStack by rootComponent.childStack.subscribeAsState()
    val authenticationState by rootComponent.authenticationState.collectAsState()
    val activeConfiguration = childComponentStack.active.configuration as Configuration
    val focusManager = LocalFocusManager.current

    LaunchedEffect(activeConfiguration) {
        focusManager.clearFocus()
    }

    UiKitTheme {
        val content = @Composable {
            AppScaffold {
                TopAppBar(
                    navigation = {
                        val isBackStackEmpty = childComponentStack.backStack.isEmpty()
                        if (isBackStackEmpty) {
                            IconButton(onClick = { }) {
                                Icon(
                                    imageVector = Lucide.House,
                                    contentDescription = "Home"
                                )
                            }
                        } else {
                            IconButton(onClick = { rootComponent.onBackClicked() }) {
                                Icon(
                                    imageVector = Lucide.ArrowLeft,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    title = {
                        Text(activeConfiguration.displayName)
                    },
                    actions = {
                        when (val activeChildComponent = childComponentStack.active.instance) {
                            is ChildComponent.Home -> {
                                HomeNavigationBarActions(homeComponent = activeChildComponent.component)
                            }

                            is ChildComponent.ListingDetails -> {}
                        }

                        if (authenticationState.isAuthenticated) {
                            IconButton(
                                onClick = { rootComponent.onAuthenticationIntent(MainStore.Intent.LogoutRequested) }
                            ) {
                                Icon(Lucide.LogOut, "Logout")
                            }
                        } else {
                            PrimaryButton(
                                onClick = { rootComponent.onAuthenticationIntent(MainStore.Intent.LoginRequested) },
                                enabled = authenticationState.isAuthenticationServiceAvailable
                            ) {
                                Icon(Lucide.LogIn, null)
                                Text("Log In")
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
                            stack = rootComponent.childStack,
                            modifier = Modifier.fillMaxSize(),
                            animation = predictiveBackAnimation(
                                backHandler = rootComponent.backHandler,
                                fallbackAnimation = stackAnimation(fade() + scale()),
                                onBack = rootComponent::onBackClicked,
                            )
                        ) { childComponent ->
                            childComponent.instance.Render(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        if (rootComponent.backDispatcher != null) {
            PredictiveBackGestureOverlay(
                backDispatcher = rootComponent.backDispatcher!!,
                backIcon = null,
                modifier = Modifier.fillMaxSize(),
                content = content
            )
        } else {
            content()
        }
    }
}

@Composable
fun ChildComponent.Render(modifier: Modifier) {
    when (this) {
        is ChildComponent.Home -> HomeScreen(
            homeComponent = this.component,
            onSearchClick = { },
            onNotificationClick = { }
        )

        is ChildComponent.ListingDetails -> ListingDetailsScreen(
            listingDetailsComponent = this.component
        )
    }
}

@Composable
private fun HomeNavigationBarActions(homeComponent: IHomeComponent) {
    val homeState by homeComponent.state.collectAsState()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (homeState.isAuthenticated) {
            IconButton(
                onClick = { homeComponent.onIntent(HomeStore.Intent.QuickCreateListingRequested) }
            ) {
                Icon(Lucide.CirclePlus, "Add")
            }
        }
    }
}