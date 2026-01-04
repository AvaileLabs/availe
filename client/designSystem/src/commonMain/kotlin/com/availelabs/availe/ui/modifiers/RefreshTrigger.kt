package com.availelabs.availe.ui.modifiers

import androidx.compose.ui.Modifier
import com.availelabs.availe.ui.RefreshStateHolder

expect fun Modifier.refreshTrigger(
    state: RefreshStateHolder,
    onRefresh: () -> Unit
): Modifier