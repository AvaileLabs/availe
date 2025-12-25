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
import availe.client.composeapp.generated.resources.navigation_home
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.availelabs.availe.auth.AuthenticationState
import com.availelabs.availe.ui.components.layout.AppTopNavigation
import com.composables.icons.lucide.House
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
                            onClick = { appShellComponent.onHomeTabClicked() },
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
                    ShellActions(
                        actions = childActions,
                        authenticationState = authenticationState,
                        isAuthenticationLoading = isAuthenticationLoading,
                        onLoginClicked = { appShellComponent.onLoginClicked() },
                        onSettingsClick = { appShellComponent.onSettingsClicked() },
                        onCancelLoginClicked = { appShellComponent.onCancelLoginClicked() }
                    )
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

                ChatOverlay(
                    appShellComponent = appShellComponent,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}