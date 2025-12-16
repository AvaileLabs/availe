package com.availelabs.availe.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.uikit.styling.*
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.ProvideTextStyle
import com.composeunstyled.outline
import com.composeunstyled.theme.Theme

@Composable
fun AppTopNavigation(
    logo: @Composable () -> Unit,
    navigationItems: @Composable RowScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Theme[colors][navigation],
    contentColor: Color = Theme[colors][onNavigation]
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(10f)
            .dropShadow(RectangleShape, Theme[shadows][subtle])
            .outline(Dp.Hairline, Theme[colors][outline])
            .background(backgroundColor)
    ) {
        ProvideContentColor(contentColor) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1200.dp)
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    ProvideTextStyle(Theme[textStyles][title].copy(fontWeight = FontWeight.Bold)) {
                        logo()
                    }

                    Spacer(Modifier.width(48.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        navigationItems()
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    actions()
                }
            }
        }
    }
}