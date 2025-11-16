package com.availelabs.availe

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.features.favorites.FavoritesScreen
import com.availelabs.availe.features.home.HomeScreen
import com.availelabs.availe.features.home.components.MockSearchBar
import com.availelabs.availe.features.listings.MyListingsScreen
import com.availelabs.availe.features.messages.MessagesScreen
import com.availelabs.availe.features.profile.ProfileScreen
import com.availelabs.availe.features.root.Child
import com.availelabs.availe.features.root.IRootComponent
import com.availelabs.availe.features.root.OverlayState
import com.availelabs.availe.features.root.RootComponent
import com.availelabs.availe.features.settings.SettingsScreen
import com.availelabs.availe.ui.ScreenScaffold
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.uikit.components.*
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme

@Composable
fun App(root: IRootComponent) {
    val childStack by root.childStack.subscribeAsState()
    val activeConfiguration = childStack.active.configuration
    val overlayState by root.overlayState.subscribeAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(root) {
        if (root is RootComponent && root.urlOpener == null) {
            root.urlOpener = { url -> uriHandler.openUri(url) }
        }
    }

    UiKitTheme {
        AppScaffold {
            ScreenScaffold(
                appBar = {
                    UiTopAppBar(
                        onSearchClick = root::onSearchClicked,
                        onNotifyClick = root::onNotificationClicked
                    )
                },
                sideBar = {
                    SideBar(
                        modifier = Modifier.fillMaxHeight(),
                        navigationItems = {
                            root.topLevelDestinations.forEach { destination ->
                                val selected = activeConfiguration == destination.configuration
                                SideBarItem(
                                    onClick = { root.onNavigate(destination.configuration) },
                                    selected = selected,
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = { Icon(destination.icon, contentDescription = null) },
                                    title = { Text(destination.name) }
                                )
                            }
                        },
                    )
                },
                bottomBar = {
                    BottomNavigationBar {
                        root.topLevelDestinations.forEach { destination ->
                            val selected = activeConfiguration == destination.configuration
                            PrimaryTab(
                                selected = selected,
                                onSelected = { root.onNavigate(destination.configuration) },
                                modifier = Modifier.weight(1f),
                                icon = {
                                    Icon(destination.icon, contentDescription = null)
                                },
                                title = {
                                    Text(
                                        text = destination.name,
                                        overflow = TextOverflow.Ellipsis,
                                        singleLine = true
                                    )
                                },
                            )
                        }
                    }
                }
            ) {
                Children(
                    stack = root.childStack,
                ) {
                    when (val child = it.instance) {
                        is Child.Home -> HomeScreen(homeScreenComponent = child.component)
                        is Child.Favorites -> FavoritesScreen()
                        is Child.MyListings -> MyListingsScreen()
                        is Child.Messages -> MessagesScreen()
                        is Child.Profile -> ProfileScreen(component = child.component)
                        is Child.Settings -> SettingsScreen()
                    }
                }

                SearchOverlay(
                    visible = overlayState is OverlayState.Search,
                    onDismiss = root::onDismissOverlay
                )

                NotificationOverlay(
                    visible = overlayState is OverlayState.Notifications,
                    onDismiss = root::onDismissOverlay
                )
            }
        }
    }
}

@Composable
fun UiTopAppBar(
    onSearchClick: () -> Unit,
    onNotifyClick: () -> Unit
) {
    TopAppBar(
        title = { Text("Availe") },
        actions = {
            IconButton(onClick = onNotifyClick) {
                Icon(Lucide.Bell, "Notifications")
            }
            IconButton(onClick = onSearchClick) {
                Icon(Lucide.Search, "Search")
            }
        }
    )
}

@Composable
fun SearchOverlay(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val animSpec = tween<Float>(durationMillis = 300)
    val fastFade = fadeIn(animSpec)
    val fastFadeOut = fadeOut(animSpec)

    AnimatedVisibility(
        visible = visible,
        enter = fastFade,
        exit = fastFadeOut,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Theme[colors][scrim])
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dropShadow(RectangleShape, Theme[shadows][elevated])
                        .background(Theme[colors][navigation])
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    MockSearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationOverlay(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val animSpec = tween<Float>(durationMillis = 300)
    val fastFade = fadeIn(animSpec)
    val fastFadeOut = fadeOut(animSpec)

    AnimatedVisibility(
        visible = visible,
        enter = fastFade,
        exit = fastFadeOut,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Theme[colors][scrim])
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .fillMaxHeight()
                        .dropShadow(RectangleShape, Theme[shadows][elevated])
                        .background(Theme[colors][navigation])
                ) {
                    Text(
                        "Notifications",
                        modifier = Modifier
                            .padding(16.dp)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    )
                }
            }
        }
    }
}