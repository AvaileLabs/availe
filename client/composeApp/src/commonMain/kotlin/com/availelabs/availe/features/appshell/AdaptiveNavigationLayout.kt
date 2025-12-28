package com.availelabs.availe.features.appshell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.availelabs.availe.auth.AuthenticationState

@Composable
expect fun AppShellLayout(
    appShellComponent: IAppShellComponent,
    childStack: ChildStack<AppShellConfiguration, AppShellChildComponent>,
    authenticationState: AuthenticationState,
    isAuthenticationLoading: Boolean,
    chatOverlay: ChildSlot<AppShellConfiguration.Chat, AppShellChildComponent.Chat>
)

@Composable
fun AppShellContentStack(
    appShellComponent: IAppShellComponent,
    isSwipeEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    ContentStack(appShellComponent, isSwipeEnabled, modifier)
}