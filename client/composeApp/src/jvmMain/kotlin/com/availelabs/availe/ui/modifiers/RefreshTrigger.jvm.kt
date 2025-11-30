package com.availelabs.availe.ui.modifiers

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import com.availelabs.availe.ui.RefreshStateHolder

actual fun Modifier.refreshTrigger(
    state: RefreshStateHolder,
    onRefresh: () -> Unit
): Modifier = composed {
    val nestedScrollConnection = remember(state, onRefresh) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero

                return if (available.y < 0 && state.pullProgress > 0) {
                    val consumed = state.onPull(available.y)
                    Offset(0f, consumed)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero

                return if (available.y > 0) {
                    val consumedY = state.onPull(available.y)
                    Offset(0f, consumedY)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (state.onRelease()) {
                    onRefresh()
                }
                return Velocity.Zero
            }
        }
    }

    this
        .onPreviewKeyEvent { event ->
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
        .nestedScroll(nestedScrollConnection)
}