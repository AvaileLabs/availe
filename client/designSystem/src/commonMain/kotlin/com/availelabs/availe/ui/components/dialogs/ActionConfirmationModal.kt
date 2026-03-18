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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.uikit.components.Icon
import com.composables.uikit.components.PrimaryButton
import com.composables.uikit.components.Text
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme
import com.composables.uikit.styling.title as TitleToken

@Composable
fun ActionConfirmationModal(
    icon: ImageVector,
    iconContentColor: Color,
    iconBackgroundColor: Color,
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonColor: Color? = null,
    confirmButtonContentColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
                    .background(iconBackgroundColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconContentColor
                )
            }

            Spacer(Modifier.height(16.dp))

            SelectionContainer {
                Text(
                    text = title,
                    style = Theme[textStyles][TitleToken],
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
                    Text(cancelText)
                }

                if (confirmButtonColor != null && confirmButtonContentColor != null) {
                    PrimaryButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        backgroundColor = confirmButtonColor,
                        contentColor = confirmButtonContentColor
                    ) {
                        Text(confirmText)
                    }
                } else {
                    PrimaryButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}