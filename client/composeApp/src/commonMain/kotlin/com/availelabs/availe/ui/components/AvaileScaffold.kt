package com.availelabs.availe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.composables.uikit.styling.background
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.onBackground
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.theme.Theme

@Composable
fun AvaileScaffold(
    modifier: Modifier = Modifier,
    appBar: (@Composable () -> Unit)? = null,
    sideBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    backgroundColor: Color = Theme[colors][background],
    contentColor: Color = Theme[colors][onBackground],
    content: @Composable BoxScope.() -> Unit,
) {
    val windowSize = rememberWindowSize()

    // Desktop: Content-only (No Nav Bars)
    // Tablet: SideBar
    // Mobile: BottomBar
    val isSideBarVisible = windowSize is WindowSize.Medium
    val isBottomBarVisible = windowSize is WindowSize.Compact

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        ProvideContentColor(contentColor) {
            if (sideBar != null && isSideBarVisible) {
                sideBar()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues())
                    .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            ) {
                if (appBar != null) {
                    appBar()
                }

                Box(modifier = Modifier.weight(1f)) {
                    content()
                }

                if (bottomBar != null && isBottomBarVisible) {
                    bottomBar()
                }
            }
        }
    }
}