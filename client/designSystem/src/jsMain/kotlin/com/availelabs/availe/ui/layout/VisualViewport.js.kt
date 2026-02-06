package com.availelabs.availe.ui.layout

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import kotlinx.browser.window
import org.w3c.dom.events.Event

@Composable
actual fun rememberVisualViewportState(): VisualViewportState {
    var viewportState by remember { mutableStateOf(VisualViewportState()) }

    DisposableEffect(Unit) {
        val visualViewport = window.asDynamic().visualViewport

        val callback = { _: Event ->
            if (visualViewport != null) {
                val heightDouble = visualViewport.height as Double
                val offsetTopDouble = visualViewport.offsetTop as Double

                // On Web, 1 CSS pixel is treated as 1 Dp in Compose layout logic.
                // We do NOT divide by density here, as that would double-scale on high-DPI screens.
                viewportState = VisualViewportState(
                    height = heightDouble.dp,
                    offsetTop = offsetTopDouble.dp,
                    isResizing = true
                )
            }
        }

        if (visualViewport != null) {
            visualViewport.addEventListener("resize", callback)
            visualViewport.addEventListener("scroll", callback)

            val initialHeightDouble = visualViewport.height as Double
            val initialOffsetTopDouble = visualViewport.offsetTop as Double

            viewportState = VisualViewportState(
                height = initialHeightDouble.dp,
                offsetTop = initialOffsetTopDouble.dp,
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