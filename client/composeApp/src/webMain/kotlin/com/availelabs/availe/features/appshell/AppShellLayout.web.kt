package com.availelabs.availe.features.appshell

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import availe.client.composeapp.generated.resources.Res
import availe.client.composeapp.generated.resources.accessibility_action_logout
import availe.client.composeapp.generated.resources.navigation_home
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.availelabs.availe.domain.auth.AuthenticationState
import com.availelabs.availe.ui.components.AppTopNavigation
import com.availelabs.availe.ui.components.AuthenticationActionButton
import com.availelabs.availe.ui.mapper.ActionIconMapper
import com.availelabs.availe.ui.model.asString
import com.composables.icons.lucide.House
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.Icon
import com.composables.uikit.components.IconButton
import com.composables.uikit.components.Text
import com.composables.uikit.styling.accent
import com.composables.uikit.styling.colors
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun AppShellLayout(
    appShellComponent: IAppShellComponent,
    childStack: ChildStack<AppShellConfiguration, AppShellChildComponent>,
    authenticationState: AuthenticationState,
    isAuthenticationLoading: Boolean,
    chatOverlay: ChildSlot<AppShellConfiguration.Chat, AppShellChildComponent.Chat>
) {
    val activeInstance = childStack.active.instance
    val childActions by activeInstance.actions.subscribeAsState()
    val activeConfiguration = childStack.active.configuration

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopNavigation(
                logo = {
                    Text("Availe", fontWeight = FontWeight.Bold)
                },
                navigationItems = {
                    val isHomeSelected =
                        activeConfiguration is AppShellConfiguration.Home || activeConfiguration.canonicalParent is AppShellConfiguration.Home

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { /* Navigate Home Logic */ },
                            backgroundColor = if (isHomeSelected) Theme[colors][accent].copy(alpha = 0.1f) else Color.Transparent
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Lucide.House, null)
                                Text(stringResource(Res.string.navigation_home))
                            }
                        }
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1200.dp)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                ) {
                    AppShellContentStack(appShellComponent)
                }

                AppShellChatOverlay(
                    appShellComponent = appShellComponent,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}