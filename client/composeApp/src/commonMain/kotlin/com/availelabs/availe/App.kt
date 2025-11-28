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
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.features.home.HomeScreen
import com.availelabs.availe.features.home.HomeStore
import com.availelabs.availe.features.home.IHomeComponent
import com.availelabs.availe.features.root.ChildComponent
import com.availelabs.availe.features.root.Configuration
import com.availelabs.availe.features.root.IRootComponent
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.LogIn
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.*
import com.composables.uikit.styling.UiKitTheme

@Composable
fun App(rootComponent: IRootComponent) {
    val childComponentStack by rootComponent.childStack.subscribeAsState()
    val activeConfiguration = childComponentStack.active.configuration as Configuration
    val focusManager = LocalFocusManager.current

    LaunchedEffect(activeConfiguration) {
        focusManager.clearFocus()
    }

    UiKitTheme {
        AppScaffold {
            TopAppBar(
                navigation = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = activeConfiguration.navigationIcon.icon,
                            contentDescription = activeConfiguration.navigationIcon.contentDescription
                        )
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

                        else -> {
                        }
                    }
                }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Children(
                        stack = rootComponent.childStack,
                        modifier = Modifier.fillMaxSize()
                    ) { childComponent ->
                        childComponent.instance.Render(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
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
                onClick = { homeComponent.onIntent(HomeStore.Intent.QuickCreateListing) }
            ) {
                Icon(Lucide.CirclePlus, "Add")
            }
            IconButton(
                onClick = { homeComponent.onIntent(HomeStore.Intent.LogoutClicked) }
            ) {
                Icon(Lucide.LogOut, "Logout")
            }
        } else {
            PrimaryButton(
                onClick = { homeComponent.onIntent(HomeStore.Intent.LoginClicked) },
                enabled = homeState.isAuthServiceAvailable
            ) {
                Icon(Lucide.LogIn, null)
                Text("Log In")
            }
        }
    }
}
