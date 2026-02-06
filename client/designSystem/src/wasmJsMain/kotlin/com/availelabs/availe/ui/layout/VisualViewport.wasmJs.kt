package com.availelabs.availe.ui.layout

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import org.w3c.dom.events.Event

// Define the external interface for VisualViewport in Wasm
@OptIn(ExperimentalWasmJsInterop::class)
private external interface VisualViewport : JsAny {
    val height: Double
    val offsetTop: Double
    fun addEventListener(type: String, callback: (Event) -> Unit)
    fun removeEventListener(type: String, callback: (Event) -> Unit)
}

// Helper to safely get visualViewport from window
@OptIn(ExperimentalWasmJsInterop::class)
private fun getVisualViewport(): VisualViewport = js("window.visualViewport")

@Composable
actual fun rememberVisualViewportState(): VisualViewportState {
    var viewportState by remember { mutableStateOf(VisualViewportState()) }

    DisposableEffect(Unit) {
        val visualViewport = getVisualViewport()

        val callback = { _: Event ->
            val heightDouble = visualViewport.height
            val offsetTopDouble = visualViewport.offsetTop

            // On Web, 1 CSS pixel is treated as 1 Dp in Compose layout logic.
            // We do NOT divide by density here, as that would double-scale on high-DPI screens.
            viewportState = VisualViewportState(
                height = heightDouble.dp,
                offsetTop = offsetTopDouble.dp,
                isResizing = true
            )
        }

        visualViewport.addEventListener("resize", callback)
        visualViewport.addEventListener("scroll", callback)

        val initialHeightDouble = visualViewport.height
        val initialOffsetTopDouble = visualViewport.offsetTop

        viewportState = VisualViewportState(
            height = initialHeightDouble.dp,
            offsetTop = initialOffsetTopDouble.dp,
            isResizing = false
        )

        onDispose {
            visualViewport.removeEventListener("resize", callback)
            visualViewport.removeEventListener("scroll", callback)
        }
    }

    return viewportState
}