package com.availelabs.availe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.features.appshell.AppShellScreen
import com.availelabs.availe.features.appshell.MainStore
import com.availelabs.availe.features.root.IRootComponent
import com.availelabs.availe.features.root.RootChildComponent
import com.availelabs.availe.ui.animation.appStackAnimation
import com.availelabs.availe.ui.components.dialogs.LoginGuardModal
import com.availelabs.availe.ui.components.feedback.ToastOverlay
import com.composables.uikit.styling.UiKitTheme
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.scrim
import com.composeunstyled.theme.Theme

@Composable
fun App(rootComponent: IRootComponent) {
    val childStack by rootComponent.childStack.subscribeAsState()
    val loginOverlay by rootComponent.loginOverlay.subscribeAsState()
    val toastMessages by rootComponent.toastMessages.subscribeAsState()

    UiKitTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Children(
                stack = childStack,
                modifier = Modifier.fillMaxSize(),
                animation = appStackAnimation(
                    backHandler = rootComponent.backHandler,
                    onBack = rootComponent::onBackClicked
                )
            ) { child ->
                when (val instance = child.instance) {
                    is RootChildComponent.AppShell -> {
                        AppShellScreen(
                            appShellComponent = instance.component
                        )
                    }

                    is RootChildComponent.Login -> { /* Handled by overlay */
                    }
                }
            }

            // Render Login Overlay
            if (loginOverlay.child != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Theme[colors][scrim])
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoginGuardModal(
                        onConfirm = {
                            rootComponent.dismissLoginOverlay()
                            rootComponent.onAuthenticationIntent(MainStore.Intent.LoginRequested)
                        },
                        onDismiss = rootComponent::dismissLoginOverlay
                    )
                }
            }

            // Render Toast Overlay
            if (toastMessages.isNotEmpty()) {
                ToastOverlay(
                    messages = toastMessages,
                    onDismiss = rootComponent::dismissToast,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}