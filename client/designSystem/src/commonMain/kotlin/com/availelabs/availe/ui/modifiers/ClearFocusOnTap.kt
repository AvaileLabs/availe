package com.availelabs.availe.ui.modifiers

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalViewConfiguration
import kotlin.math.hypot

fun Modifier.clearFocusOnUnconsumedTap(
    enabled: Boolean = true,
    force: Boolean = false,
): Modifier = composed {
    if (!enabled) return@composed this

    val focusManager = LocalFocusManager.current
    val viewConfiguration = LocalViewConfiguration.current

    pointerInput(enabled, force) {
        awaitEachGesture {
            val downEvent = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Final
            )

            val startPosition = downEvent.position
            val touchSlop = viewConfiguration.touchSlop

            var anyChangeConsumed = downEvent.isConsumed
            var movedBeyondSlop = false

            while (true) {
                val pointerEvent = awaitPointerEvent(PointerEventPass.Final)

                if (pointerEvent.changes.any { it.isConsumed }) {
                    anyChangeConsumed = true
                }

                val pointerInputChange = pointerEvent.changes.firstOrNull { it.id == downEvent.id }
                    ?: pointerEvent.changes.first()

                if (pointerInputChange.positionChanged()) {
                    val deltaX = pointerInputChange.position.x - startPosition.x
                    val deltaY = pointerInputChange.position.y - startPosition.y
                    if (hypot(deltaX.toDouble(), deltaY.toDouble()) > touchSlop) {
                        movedBeyondSlop = true
                    }
                }

                val pointerUp = pointerEvent.changes.any {
                    it.id == downEvent.id && it.changedToUpIgnoreConsumed()
                }
                val allPointersUp = pointerEvent.changes.all { !it.pressed }

                if (pointerUp || allPointersUp) {
                    if (!anyChangeConsumed && !movedBeyondSlop) {
                        focusManager.clearFocus(force = force)
                    }
                    break
                }
            }
        }
    }
}