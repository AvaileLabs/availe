package com.availelabs.availe.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import availe.client.designsystem.generated.resources.*
import com.availelabs.availe.settings.ISettingsStore.Intent
import com.availelabs.availe.ui.layout.WindowSizeClass
import com.availelabs.availe.ui.layout.rememberWindowSize
import com.availelabs.availe.ui.model.asString
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.User
import com.composables.uikit.components.*
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    component: ISettingsComponent
) {
    val state by component.state.collectAsState()
    val windowSize = rememberWindowSize()

    LaunchedEffect(Unit) {
        component.onIntent(Intent.RefreshPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme[colors][background]),
        contentAlignment = Alignment.TopCenter
    ) {
        if (state.refreshing) {
            IntermediateProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            when (windowSize) {
                WindowSizeClass.Compact -> {
                    MobileSettingsLayout(
                        state = state,
                        onIntent = component::onIntent
                    )
                }

                else -> {
                    DesktopSettingsLayout(
                        state = state,
                        onIntent = component::onIntent
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileSettingsLayout(
    state: ISettingsStore.State,
    onIntent: (Intent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(24.dp)
        ) {
            SettingsIdentitySection(state.displayName.value)

            Spacer(modifier = Modifier.height(32.dp))

            SettingsActionsSection(onIntent)

            SettingsErrorSection(state.inlineError)
        }
    }
}

@Composable
private fun DesktopSettingsLayout(
    state: ISettingsStore.State,
    onIntent: (Intent) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Identity Column
        Card(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(32.dp)
        ) {
            SettingsIdentitySection(state.displayName.value)
        }

        // Actions Column
        Column(
            modifier = Modifier.weight(2f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(24.dp)
            ) {
                SettingsManageAccountSection(onIntent)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(24.dp)
            ) {
                SettingsSessionSection(onIntent)
            }

            SettingsErrorSection(state.inlineError)
        }
    }
}

@Composable
private fun SettingsIdentitySection(displayName: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Theme[colors][secondary]),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.User,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Theme[colors][onSecondary]
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SelectionContainer {
            Text(
                text = stringResource(Res.string.settings_greeting, displayName),
                style = Theme[textStyles][title],
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SettingsActionsSection(onIntent: (Intent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsManageAccountSection(onIntent)

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSessionSection(onIntent)
    }
}

@Composable
private fun SettingsManageAccountSection(onIntent: (Intent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.Settings, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.settings_section_account),
                style = Theme[textStyles][title].copy(fontSize = 18.sp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SelectionContainer {
            Text(
                text = stringResource(Res.string.settings_manage_account_description),
                style = Theme[textStyles][body],
                color = Theme[colors][onCard].copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SelectionContainer {
            Text(
                text = stringResource(Res.string.settings_manage_account_hint),
                style = Theme[textStyles][caption],
                color = Theme[colors][onCard].copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            onClick = { onIntent(Intent.EnterKeycloakPage) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.settings_manage_account))
        }
    }
}

@Composable
private fun SettingsSessionSection(onIntent: (Intent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.LogOut, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.settings_section_session),
                style = Theme[textStyles][title].copy(fontSize = 18.sp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { onIntent(Intent.Logout) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.settings_logout))
        }
    }
}

@Composable
private fun SettingsErrorSection(error: com.availelabs.availe.ui.model.UiText?) {
    error?.let {
        Spacer(modifier = Modifier.height(16.dp))
        SelectionContainer {
            Text(
                text = it.asString(),
                color = Theme[colors][destructive],
                style = Theme[textStyles][caption],
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}