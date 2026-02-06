package com.availelabs.availe.ui.layout

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import kotlinx.browser.window
import org.w3c.dom.events.Event

@Composable
actual fun rememberVisualViewportState(): VisualViewportState {
    var viewportState by remember { mutableStateOf(VisualViewportState()) }
    val density = LocalDensity.current

    DisposableEffect(density) {
        val visualViewport = window.asDynamic().visualViewport

        val callback = { _: Event ->
            if (visualViewport != null) {
                val heightDouble = visualViewport.height as Double
                val offsetTopDouble = visualViewport.offsetTop as Double

                val height = with(density) { heightDouble.toFloat().toDp() }
                val offsetTop = with(density) { offsetTopDouble.toFloat().toDp() }

                viewportState = VisualViewportState(
                    height = height,
                    offsetTop = offsetTop,
                    isResizing = true
                )
            }
        }

        if (visualViewport != null) {
            visualViewport.addEventListener("resize", callback)
            visualViewport.addEventListener("scroll", callback)

            val initialHeightDouble = visualViewport.height as Double
            val initialOffsetTopDouble = visualViewport.offsetTop as Double

            val initialHeight = with(density) { initialHeightDouble.toFloat().toDp() }
            val initialOffset = with(density) { initialOffsetTopDouble.toFloat().toDp() }

            viewportState = VisualViewportState(
                height = initialHeight,
                offsetTop = initialOffset,
                isResizing = false
            )
        }

        onDispose {
            if (visualViewport != null) {
                visualViewport.removeEventListener("resize", callback)
                visualViewport.removeEventListener("scroll", callback)
            }
        }
    }

    return viewportState
}