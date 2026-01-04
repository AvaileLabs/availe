package com.composables.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.composables.uikit.styling.caption
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.onSecondary
import com.composables.uikit.styling.secondary
import com.composables.uikit.styling.textStyles
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.ProvideTextStyle
import com.composeunstyled.theme.Theme

@Composable
fun Chip(
    shape: Shape = RoundedCornerShape(100),
    backgroundColor: Color = Theme[colors][secondary].copy(0.60f),
    contentColor: Color = Theme[colors][onSecondary],
    contentPadding: PaddingValues = PaddingValues(8.dp),
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(shape)
            .background(backgroundColor)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProvideContentColor(contentColor) {
            ProvideTextStyle(Theme[textStyles][caption]) {
                content()
            }
        }
    }
}