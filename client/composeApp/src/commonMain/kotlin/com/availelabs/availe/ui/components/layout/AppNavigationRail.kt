package com.availelabs.availe.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.navigation
import com.composables.uikit.styling.onNavigation
import com.composables.uikit.styling.outline
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.theme.Theme

@Composable
fun AppNavigationRail(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    navigationItems: @Composable ColumnScope.() -> Unit,
) {
    val outlineColor = Theme[colors][outline]
    Column(
        modifier
            .zIndex(5f)
            .fillMaxHeight()
            .width(80.dp)
            .background(Theme[colors][navigation])
            .drawBehind {
                val strokeWidth = 1.dp
                drawLine(
                    color = outlineColor,
                    start = Offset(size.width - strokeWidth.toPx(), 0f),
                    end = Offset(size.width - strokeWidth.toPx(), size.height),
                    strokeWidth = strokeWidth.toPx()
                )
            }
            .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Top))
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProvideContentColor(Theme[colors][onNavigation]) {
            if (header != null) {
                Spacer(Modifier.height(16.dp))
                header()
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                navigationItems()
            }
        }
    }
}