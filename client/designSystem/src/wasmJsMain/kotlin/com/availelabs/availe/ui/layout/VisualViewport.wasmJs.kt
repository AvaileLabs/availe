package com.availelabs.availe.ui.layout

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
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
    val density = LocalDensity.current

    DisposableEffect(density) {
        val visualViewport = getVisualViewport()

        val callback = { _: Event ->
            val heightDouble = visualViewport.height
            val offsetTopDouble = visualViewport.offsetTop

            val height = with(density) { heightDouble.toFloat().toDp() }
            val offsetTop = with(density) { offsetTopDouble.toFloat().toDp() }

            viewportState = VisualViewportState(
                height = height,
                offsetTop = offsetTop,
                isResizing = true
            )
        }

        visualViewport.addEventListener("resize", callback)
        visualViewport.addEventListener("scroll", callback)

        val initialHeightDouble = visualViewport.height
        val initialOffsetTopDouble = visualViewport.offsetTop

        val initialHeight = with(density) { initialHeightDouble.toFloat().toDp() }
        val initialOffset = with(density) { initialOffsetTopDouble.toFloat().toDp() }

        viewportState = VisualViewportState(
            height = initialHeight,
            offsetTop = initialOffset,
            isResizing = false
        )

        onDispose {
            visualViewport.removeEventListener("resize", callback)
            visualViewport.removeEventListener("scroll", callback)
        }
    }

    return viewportState
}