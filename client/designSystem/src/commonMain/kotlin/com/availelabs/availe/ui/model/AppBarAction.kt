package com.availelabs.availe.ui.model

data class AppBarAction(
    val icon: ActionIcon,
    val label: UiText,
    val onClick: () -> Unit
)