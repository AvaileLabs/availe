package com.availelabs.availe.ui.model

import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource

data class NavigationUiState(
    val title: StringResource,
    val icon: ImageVector,
    val contentDescription: StringResource
)