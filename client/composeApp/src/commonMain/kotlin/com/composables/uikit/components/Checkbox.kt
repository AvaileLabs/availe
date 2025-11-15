package com.composables.uikit.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.uikit.styling.accent
import com.composables.uikit.styling.bright
import com.composables.uikit.styling.card
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.dim
import com.composables.uikit.styling.focusRing
import com.composables.uikit.styling.indications
import com.composables.uikit.styling.isBright
import com.composables.uikit.styling.mutate
import com.composables.uikit.styling.onAccent
import com.composables.uikit.styling.outline
import com.composables.uikit.styling.shapes
import com.composables.uikit.styling.small
import com.composeunstyled.minimumInteractiveComponentSize
import com.composeunstyled.outline
import com.composeunstyled.theme.Theme
import com.composeunstyled.Checkbox as UnstyledCheckbox

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = Theme[shapes][small],
    activatedBackgroundColor: Color = Theme[colors][accent],
    backgroundColor: Color = Theme[colors][card],
    contentColor: Color = Theme[colors][onAccent],
) {
    if (isBright(backgroundColor)) Theme[indications][bright] else Theme[indications][dim]
    val bgColor = if (checked) activatedBackgroundColor else backgroundColor
    val focused by interactionSource.collectIsFocusedAsState()
    val outlineColor = if (focused) Theme[colors][focusRing] else Theme[colors][outline]

    UnstyledCheckbox(
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        modifier = modifier
            .size(24.dp)
            .outline(1.dp, outlineColor, shape),
        shape = shape,
        backgroundColor = bgColor.mutate(enabled),
        contentColor = contentColor.mutate(enabled),
        interactionSource = interactionSource
    ) {
        Icon(Lucide.Check, null)
    }
}

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = Theme[shapes][small],
    contentPadding: PaddingValues = PaddingValues(0.dp),
    label: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .clip(shape)
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
                indication = LocalIndication.current
            )
            .minimumInteractiveComponentSize()
            .padding(contentPadding)
            // additional padding so that the checkbox' outline is not clipped
            .padding(1.dp)
        ,
    ) {
        if (label != null) {
            label()
        }

        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            interactionSource = interactionSource
        )
    }
}
