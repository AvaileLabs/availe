package com.availelabs.availe.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.composables.uikit.components.DropdownMenu
import com.composables.uikit.components.DropdownMenuPanel
import com.composeunstyled.DropdownPanelAnchor

@Composable
fun ContextMenuArea(
    modifier: Modifier = Modifier,
    menuItems: @Composable ColumnScope.(onDismiss: () -> Unit) -> Unit,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            val change = event.changes.first()
                            offset = with(density) { DpOffset(change.position.x.toDp(), change.position.y.toDp()) }
                            isExpanded = true
                            change.consume()
                        }
                    }
                }
            }
    ) {
        content()

        if (isExpanded) {
            Box(modifier = Modifier.offset(offset.x, offset.y)) {
                DropdownMenu(
                    onExpandRequest = { isExpanded = true }
                ) {
                    DropdownMenuPanel(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false },
                        anchor = DropdownPanelAnchor.BottomStart,
                        modifier = Modifier.widthIn(min = 200.dp, max = 240.dp)
                    ) {
                        menuItems { isExpanded = false }
                    }
                }
            }
        }
    }
}