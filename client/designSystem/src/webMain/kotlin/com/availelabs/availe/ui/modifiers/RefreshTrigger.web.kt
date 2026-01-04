package com.availelabs.availe.ui.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import com.availelabs.availe.ui.RefreshStateHolder

actual fun Modifier.refreshTrigger(
    state: RefreshStateHolder,
    onRefresh: () -> Unit
): Modifier {
    return this.onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown) {
            if (event.key == Key.F5) {
                onRefresh()
                return@onPreviewKeyEvent true
            }
            if (event.key == Key.R && (event.isCtrlPressed || event.isMetaPressed)) {
                onRefresh()
                return@onPreviewKeyEvent true
            }
        }
        false
    }
}