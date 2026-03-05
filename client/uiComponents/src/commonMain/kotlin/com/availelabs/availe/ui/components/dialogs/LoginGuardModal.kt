package com.availelabs.availe.ui.components.dialogs

import androidx.compose.runtime.Composable
import availe.client.designsystem.generated.resources.*
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.onBackground
import com.composables.uikit.styling.outline
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginGuardModal(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DimmedDialog(onDismissRequest = onDismiss) {
        ActionConfirmationModal(
            icon = Lucide.Lock,
            iconContentColor = Theme[colors][onBackground],
            iconBackgroundColor = Theme[colors][outline].copy(alpha = 0.1f),
            title = stringResource(Res.string.authentication_guard_title),
            message = stringResource(Res.string.authentication_guard_body),
            confirmText = stringResource(Res.string.authentication_sign_in),
            cancelText = stringResource(Res.string.generic_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}