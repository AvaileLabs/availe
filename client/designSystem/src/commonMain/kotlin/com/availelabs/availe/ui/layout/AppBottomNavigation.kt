package com.availelabs.availe.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.uikit.components.DisableFontScaling
import com.composables.uikit.styling.*
import com.composeunstyled.outline
import com.composeunstyled.theme.Theme

@Composable
fun AppBottomNavigation(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    backgroundColor: Color = Theme[colors][navigation],
    contentColor: Color = Theme[colors][onNavigation],
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .dropShadow(shape, Theme[shadows][elevated])
            .outline(Dp.Hairline, Theme[colors][outline])
            .clip(shape)
            .background(backgroundColor)
            .zIndex(5f)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DisableFontScaling {
                content()
            }
        }
    }
}