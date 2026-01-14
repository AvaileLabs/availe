package com.availelabs.availe.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.generic_cancel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.TriangleAlert
import com.composables.uikit.components.Icon
import com.composables.uikit.components.PrimaryButton
import com.composables.uikit.components.Text
import com.composables.uikit.styling.*
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
    Box(
        modifier = Modifier
            .width(320.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Theme[shadows][modal].color
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Theme[colors][card])
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Theme[colors][destructive].copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.TriangleAlert,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Theme[colors][destructive]
                )
            }

            Spacer(Modifier.height(16.dp))

            SelectionContainer {
                Text(
                    text = title,
                    style = Theme[textStyles][com.composables.uikit.styling.title],
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(8.dp))

            SelectionContainer {
                Text(
                    text = message,
                    style = Theme[textStyles][body].copy(
                        color = Theme[colors][onBackground].copy(alpha = 0.7f)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    backgroundColor = Theme[colors][card],
                    contentColor = Theme[colors][onBackground],
                    borderWidth = 1.dp,
                    borderColor = Theme[colors][outline].copy(alpha = 0.2f)
                ) {
                    Text(stringResource(Res.string.generic_cancel))
                }

                PrimaryButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    backgroundColor = Theme[colors][destructive],
                    contentColor = Theme[colors][onPrimary]
                ) {
                    Text(confirmText)
                }
            }
        }
    }
}