package com.availelabs.availe.features.appshell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import availe.client.composeapp.generated.resources.Res
import availe.client.composeapp.generated.resources.chat_title
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.features.chat.ChatScreen
import com.availelabs.availe.ui.animation.appStackAnimation
import com.availelabs.availe.ui.components.ChatOverlayWindow
import com.availelabs.availe.ui.components.PredictiveBackGestureOverlay
import com.availelabs.availe.ui.components.WindowSizeClass
import com.availelabs.availe.ui.components.rememberWindowSize
import com.availelabs.availe.ui.model.LayoutMode
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

    val layoutMode = when (windowSize) {
        WindowSizeClass.Expanded -> LayoutMode.MultiPane
        else -> LayoutMode.SinglePane
    }

    LaunchedEffect(layoutMode) {
        appShellComponent.onLayoutModeChanged(layoutMode)
    }

    LaunchedEffect(activeConfiguration) {
        focusManager.clearFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PredictiveBackGestureOverlay(
            backDispatcher = backDispatcher,
            backIcon = null,
            modifier = Modifier.fillMaxSize(),
            content = {
                AppShellLayout(
                    appShellComponent = appShellComponent,
                    childStack = childStack,
                    authenticationState = authenticationState,
                    isAuthenticationLoading = isAuthenticationLoading,
                    chatOverlay = chatOverlay
                )
            }
        )
    }
}

@OptIn(ExperimentalDecomposeApi::class)
@Composable
internal fun ContentStack(
    appShellComponent: IAppShellComponent,
    modifier: Modifier = Modifier
) {
    val childStack by appShellComponent.childStack.subscribeAsState()
    Children(
        stack = childStack,
        modifier = modifier.fillMaxSize(),
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

@Composable
internal fun BoxScope.ChatOverlay(
    appShellComponent: IAppShellComponent,
    modifier: Modifier = Modifier
) {
    val chatOverlay by appShellComponent.chatOverlay.subscribeAsState()
    val overlayInstance = chatOverlay.child?.instance

    if (overlayInstance != null) {
        Box(
            modifier = modifier.padding(end = 24.dp, bottom = 24.dp)
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