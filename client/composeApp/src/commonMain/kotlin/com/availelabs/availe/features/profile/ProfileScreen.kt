package com.availelabs.availe.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.ui.ScreenScaffold
import com.composables.icons.lucide.*
import com.composables.uikit.components.*
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.destructive
import com.composables.uikit.styling.textStyles
import com.composables.uikit.styling.title
import com.composeunstyled.theme.Theme

@Composable
fun ProfileScreen(component: IProfileComponent) {
    val state by component.uiState.subscribeAsState()

    ScreenScaffold {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.initializationError != null -> {
                    ErrorContent(
                        message = state.initializationError!!,
                        onRetry = { component.onEvent(ProfileEvents.LoginClicked) }
                    )
                }

                state.isAuthenticated -> {
                    AuthenticatedContent(
                        profile = state.userProfile,
                        onManageAccount = { component.onEvent(ProfileEvents.ManageAccountClicked) },
                        onLogout = { component.onEvent(ProfileEvents.LogoutClicked) }
                    )
                }

                else -> {
                    GuestContent(
                        isLoading = state.isLoading,
                        onLogin = { component.onEvent(ProfileEvents.LoginClicked) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Lucide.CircleAlert,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = Theme[colors][destructive]
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Authentication Error",
            style = Theme[textStyles][title],
            textAlign = TextAlign.Center
        )

        Text(
            text = message,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(32.dp))

        PrimaryButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun AuthenticatedContent(
    profile: UserProfile?,
    onManageAccount: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val initials = when (profile) {
            is UserProfile.Named -> profile.name
            is UserProfile.Anonymous -> "User"
            null -> "U"
        }

        Avatar(
            painter = rememberPlaceholderPainter(),
            initials = initials,
            modifier = Modifier.size(96.dp)
        )

        Spacer(Modifier.height(24.dp))

        val displayName = when (profile) {
            is UserProfile.Named -> profile.name
            is UserProfile.Anonymous -> "User"
            null -> "Unknown User"
        }

        Text(
            text = displayName,
            style = Theme[textStyles][title],
            textAlign = TextAlign.Center
        )

        Text(
            text = profile?.email ?: "",
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        SecondaryButton(
            onClick = onManageAccount,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Icon(Lucide.UserCog, null)
            Text("Manage Account")
        }

        Spacer(Modifier.height(16.dp))

        DestructiveButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Icon(Lucide.LogOut, null)
            Text("Log Out")
        }
    }
}

@Composable
private fun GuestContent(
    isLoading: Boolean,
    onLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Lucide.User,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Welcome to Availe",
            style = Theme[textStyles][title],
            textAlign = TextAlign.Center
        )

        Text(
            text = "Sign in to view your profile and manage your account settings.",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(32.dp))

        PrimaryButton(
            onClick = onLogin,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(if (isLoading) "Signing in..." else "Sign In")
        }
    }
}

@Composable
private fun rememberPlaceholderPainter(): Painter {
    return remember { ColorPainter(Color.Transparent) }
}