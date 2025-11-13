package com.availelabs.availe.ui.composables.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.uikit.styling.background
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.onBackground
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.currentWindowContainerSize
import com.composeunstyled.theme.Theme

@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    appBar: (@Composable () -> Unit)? = null,
    sideBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    backgroundColor: Color = Theme[colors][background],
    contentColor: Color = Theme[colors][onBackground],
    content: @Composable BoxScope.() -> Unit,
) {
    val isWide = currentWindowContainerSize().width > 600.dp
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        ProvideContentColor(contentColor) {
            if (sideBar != null && isWide) {
                sideBar()
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues())
                    .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                if (appBar != null) {
                    appBar()
                }
                Box(Modifier.weight(1f)) {
                    content()
                }
                if (isWide.not() && bottomBar != null) {
                    bottomBar()
                }
            }
        }
    }
}