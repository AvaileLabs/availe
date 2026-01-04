package com.composables.uikit.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.core.HorizontalSeparator
import com.composables.core.Separator
import com.composables.core.VerticalSeparator
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.outline
import com.composeunstyled.theme.Theme

@Composable
fun ColumnScope.Divider(modifier: Modifier = Modifier, color: Color = Theme[colors][outline], thickness: Dp = 1.dp) {
    Separator(color = color, thickness = thickness)
}

@Composable
fun RowScope.Divider(modifier: Modifier = Modifier, color: Color = Theme[colors][outline], thickness: Dp = 1.dp) {
    Separator(color = color, thickness = thickness)
}

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, color: Color = Theme[colors][outline], thickness: Dp = 1.dp) {
    HorizontalSeparator(color = color, thickness = thickness)
}

@Composable
fun VerticalDivider(modifier: Modifier = Modifier, color: Color = Theme[colors][outline], thickness: Dp = 1.dp) {
    VerticalSeparator(color = color, thickness = thickness)
}