package com.availelabs.availe.ui.navigation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.uikit.styling.*
import com.composeunstyled.UnstyledButton
import com.composeunstyled.LocalContentColor
import com.composeunstyled.outline
import com.composeunstyled.theme.Theme

@Composable
fun ShellNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val outlineColor = if (focused) Theme[colors][focusRing] else Color.Transparent
    val shape: Shape = Theme[shapes][medium]

    UnstyledButton(
        onClick = onClick,
        modifier = modifier
            .semantics { role = Role.Tab }
            .outline(2.dp, outlineColor, shape),
        shape = shape,
        contentPadding = PaddingValues(8.dp),
        contentColor = if (selected) Theme[colors][accent] else LocalContentColor.current,
        interactionSource = interactionSource,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            icon()
            if (label != null) {
                Spacer(Modifier.height(4.dp))
                label()
            }
        }
    }
}

@Composable
fun ShellRailNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val outlineColor = if (focused) Theme[colors][focusRing] else Color.Transparent
    val shape: Shape = Theme[shapes][medium]

    UnstyledButton(
        onClick = onClick,
        modifier = modifier
            .semantics { role = Role.Tab }
            .outline(2.dp, outlineColor, shape),
        shape = shape,
        contentPadding = PaddingValues(12.dp),
        contentColor = if (selected) Theme[colors][primary] else LocalContentColor.current,
        backgroundColor = if (selected) Theme[colors][accent].copy(alpha = 0.1f) else Color.Transparent,
        interactionSource = interactionSource,
    ) {
        icon()
    }
}