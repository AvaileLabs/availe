package com.composables.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.uikit.styling.background
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.onBackground
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.theme.Theme

@Composable
fun AppScaffold(
    backgroundColor: Color = Theme[colors][background],
    contentColor: Color = Theme[colors][onBackground],
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(backgroundColor).padding(contentPadding)) {
        ProvideContentColor(contentColor) {
            content()
        }
    }
}