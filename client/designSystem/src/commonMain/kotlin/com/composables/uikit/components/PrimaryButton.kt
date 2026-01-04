package com.composables.uikit.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.uikit.styling.body
import com.composables.uikit.styling.bright
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.dim
import com.composables.uikit.styling.focusRing
import com.composables.uikit.styling.indications
import com.composables.uikit.styling.isBright
import com.composables.uikit.styling.mutate
import com.composables.uikit.styling.onPrimary
import com.composables.uikit.styling.primary
import com.composables.uikit.styling.shapes
import com.composables.uikit.styling.small
import com.composables.uikit.styling.textStyles
import com.composeunstyled.ProvideTextStyle
import com.composeunstyled.focusRing
import com.composeunstyled.minimumInteractiveComponentSize
import com.composeunstyled.theme.Theme
import com.composeunstyled.Button as UnstyledButton

val DefaultButtonPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
val DefaultIconButtonPadding = PaddingValues(8.dp)

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Theme[colors][primary],
    contentColor: Color = Theme[colors][onPrimary],
    shape: Shape = Theme[shapes][small],
    contentPadding: PaddingValues = DefaultButtonPadding,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 1.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit,
) {
    val overriddenBackgroundColor = backgroundColor.mutate(enabled)
    val indication = if (isBright(backgroundColor)) Theme[indications][dim] else Theme[indications][bright]

    UnstyledButton(
        enabled = enabled,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .focusRing(interactionSource, 2.dp, color = Theme[colors][focusRing], shape),
        shape = shape,
        onClick = onClick,
        borderColor = borderColor,
        borderWidth = borderWidth,
        backgroundColor = overriddenBackgroundColor,
        interactionSource = interactionSource,
        contentColor = contentColor,
        indication = indication,
    ) {
        ProvideTextStyle(Theme[textStyles][body].copy(fontWeight = FontWeight.Medium)) {
            content()
        }
    }
}
