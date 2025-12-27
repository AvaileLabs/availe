package com.availelabs.availe.features.appshell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import availe.client.composeapp.generated.resources.Res
import availe.client.composeapp.generated.resources.accessibility_action_settings
import availe.client.composeapp.generated.resources.chat_title
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.features.chat.ChatScreen
import com.availelabs.availe.features.home.HomeScreen
import com.availelabs.availe.features.listingdetails.ListingDetailsScreen
import com.availelabs.availe.features.listingeditor.ui.ListingEditorScreen
import com.availelabs.availe.features.profile.ProfileScreen
import com.availelabs.availe.features.settings.SettingsScreen
import com.availelabs.availe.ui.animation.appStackAnimation
import com.availelabs.availe.ui.components.PredictiveBackGestureOverlay
import com.availelabs.availe.ui.components.dialogs.ChatOverlayWindow
import com.availelabs.availe.ui.components.feedback.AuthenticationActionButton
import com.availelabs.availe.ui.components.layout.WindowSizeClass
import com.availelabs.availe.ui.components.layout.rememberWindowSize
import com.availelabs.availe.ui.mapper.ActionIconMapper
import com.availelabs.availe.ui.model.AppBarAction
import com.availelabs.availe.ui.model.LayoutMode
import com.availelabs.availe.ui.model.asString
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.composables.uikit.components.Icon
import com.composables.uikit.components.IconButton
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
internal fun ChatOverlay(
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
fun ShellActions(
    actions: List<AppBarAction>,
    authenticationState: AuthenticationState,
    isAuthenticationLoading: Boolean,
    onLoginClicked: () -> Unit,
    onSettingsClick: () -> Unit,
    onCancelLoginClicked: () -> Unit
) {
    actions.forEach { action ->
        val iconVector = ActionIconMapper.map(action.icon)
        IconButton(onClick = action.onClick) {
            Icon(iconVector, action.label.asString())
        }
    }

    if (authenticationState is AuthenticationState.LoggedIn) {
        IconButton(
            onClick = onSettingsClick
        ) {
            Icon(Lucide.Settings, stringResource(Res.string.accessibility_action_settings))
        }
    } else {
        AuthenticationActionButton(
            isLoading = isAuthenticationLoading,
            onLoginClicked = onLoginClicked,
            onCancelLoginClicked = onCancelLoginClicked
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

        is AppShellChildComponent.Settings -> SettingsScreen(
            component = this.component
        )

        is AppShellChildComponent.ListingEditor -> ListingEditorScreen(
            component = this.component
        )

        is AppShellChildComponent.Profile -> ProfileScreen(
            component = this.component
        )
    }
}