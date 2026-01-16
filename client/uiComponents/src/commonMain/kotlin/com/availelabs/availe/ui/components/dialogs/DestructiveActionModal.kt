package com.availelabs.availe.ui.components.dialogs

import androidx.compose.runtime.Composable
import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.generic_cancel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.TriangleAlert
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.destructive
import com.composables.uikit.styling.onPrimary
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
fun DestructiveActionModal(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionConfirmationModal(
        icon = Lucide.TriangleAlert,
        iconContentColor = Theme[colors][destructive],
        iconBackgroundColor = Theme[colors][destructive].copy(alpha = 0.1f),
        title = title,
        message = message,
        confirmText = confirmText,
        cancelText = stringResource(Res.string.generic_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmButtonColor = Theme[colors][destructive],
        confirmButtonContentColor = Theme[colors][onPrimary]
    )
}