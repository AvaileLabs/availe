package com.composables.uikit.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.uikit.styling.*
import com.composeunstyled.*
import com.composeunstyled.theme.Theme

@Composable
fun IconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    shape: Shape = Theme[shapes][small],
    contentColor: Color = LocalContentColor.current,
    backgroundColor: Color = Color.Transparent,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 1.dp,
    contentPadding: PaddingValues = DefaultIconButtonPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    val overriddenContentColor = contentColor.mutate(enabled)
    val overriddenBackgroundColor = backgroundColor.mutate(enabled)
    val borderColor = when {
        borderColor.isUnspecified || borderColor == Color.Transparent -> Color.Transparent
        enabled -> borderColor
        else -> borderColor.copy(alpha = 0.50f)
    }
    val indication = if (isBright(overriddenBackgroundColor)) Theme[indications][bright] else Theme[indications][dim]
    UnstyledButton(
        onClick = onClick,
        shape = shape,
        backgroundColor = overriddenBackgroundColor,
        modifier = modifier.minimumInteractiveComponentSize()
            .focusRing(interactionSource, 2.dp, color = Theme[colors][focusRing], shape),
        enabled = enabled,
        borderColor = borderColor,
        borderWidth = borderWidth,
        indication = indication,
        contentColor = overriddenContentColor,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    ) {
        ProvideTextStyle(Theme[textStyles][body].copy(fontWeight = FontWeight.Medium)) {
            content()
        }
    }
}
