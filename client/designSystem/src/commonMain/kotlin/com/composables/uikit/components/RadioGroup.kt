package com.composables.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.unit.dp
import com.composables.uikit.styling.*
import com.composeunstyled.*
import com.composeunstyled.theme.Theme


@Composable
fun RadioGroup(
    value: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    contentDescription: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier) {
        if (title != null) {
            ProvideTextStyle(Theme[textStyles][com.composables.uikit.styling.title]) {
                title()
                Spacer(Modifier.height(24.dp))
            }
        }
        UnstyledRadioGroup(
            value = value,
            onValueChange = onValueChange,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun RadioButton(
    option: String,
    selected: Boolean,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val outlineColor = if (focused) Theme[colors][focusRing] else Theme[colors][outline]

    UnstyledRadioButton(
        value = option,
        shape = Theme[shapes][medium],
        backgroundColor = Theme[colors][card],
        contentColor = Theme[colors][onCard],
        modifier = Modifier.fillMaxWidth().outline(2.dp, outlineColor, Theme[shapes][medium])
            .minimumInteractiveComponentSize(),
        contentPadding = PaddingValues(16.dp),
        interactionSource = interactionSource,
    ) {
        RadioIcon(selected)
        Spacer(Modifier.width(16.dp))
        Column {
            content()
        }
    }
}

@Composable
private fun RadioIcon(selected: Boolean) {
    Box(
        Modifier
            .size(20.dp)
            .dropShadow(shape = CircleShape, shadow = Theme[shadows][subtle])
            .outline(1.dp, Theme[colors][outline], CircleShape)
            .background(if (selected) Theme[colors][primary] else Theme[colors][card], CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(Theme[colors][onPrimary], CircleShape)
            )
        }
    }
}