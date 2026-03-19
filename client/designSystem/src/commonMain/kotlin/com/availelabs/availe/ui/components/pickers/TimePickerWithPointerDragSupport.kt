package com.availelabs.availe.ui.components.pickers

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.uikit.styling.*
import com.composeunstyled.Text
import com.composeunstyled.focusRing
import com.composeunstyled.theme.Theme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

@Composable
fun TimePickerWithPointerDragSupport(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    use24HourFormat: Boolean = true,
    showSecondsPicker: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (use24HourFormat) {
        TimePicker24HourWithPointerDragSupport(
            time = time,
            onTimeChange = onTimeChange,
            showSecondsPicker = showSecondsPicker,
            modifier = modifier
        )
    } else {
        TimePicker12HourWithPointerDragSupport(
            time = time,
            onTimeChange = onTimeChange,
            showSecondsPicker = showSecondsPicker,
            modifier = modifier
        )
    }
}

@Composable
private fun TimePicker24HourWithPointerDragSupport(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    showSecondsPicker: Boolean,
    modifier: Modifier = Modifier,
) {
    val currentHour by remember(time) { mutableStateOf(time.hour) }
    val currentMinute by remember(time) { mutableStateOf(time.minute) }
    val currentSecond by remember(time) { mutableStateOf(time.second) }

    val hours = (0..23).toList()
    val hourLabels = hours.map { it.toString().padStart(2, '0') }

    val minutes = (0..59).toList()
    val minuteLabels = minutes.map { it.toString().padStart(2, '0') }

    val seconds = (0..59).toList()
    val secondLabels = seconds.map { it.toString().padStart(2, '0') }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PickerWithPointerDragSupport(
            value = currentHour.toString().padStart(2, '0'),
            values = hourLabels,
            onValueChange = { newHour ->
                onTimeChange(LocalTime(newHour.toInt(), currentMinute, currentSecond))
            },
            modifier = Modifier.width(60.dp)
        )

        PickerWithPointerDragSupport(
            value = currentMinute.toString().padStart(2, '0'),
            values = minuteLabels,
            onValueChange = { newMinute ->
                onTimeChange(LocalTime(currentHour, newMinute.toInt(), currentSecond))
            },
            modifier = Modifier.width(60.dp)
        )

        if (showSecondsPicker) {
            PickerWithPointerDragSupport(
                value = currentSecond.toString().padStart(2, '0'),
                values = secondLabels,
                onValueChange = { newSecond ->
                    onTimeChange(LocalTime(currentHour, currentMinute, newSecond.toInt()))
                },
                modifier = Modifier.width(60.dp)
            )
        }
    }
}

@Composable
private fun TimePicker12HourWithPointerDragSupport(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    showSecondsPicker: Boolean,
    modifier: Modifier = Modifier,
) {
    val currentHour by remember(time) { mutableStateOf(time.hour) }
    val currentMinute by remember(time) { mutableStateOf(time.minute) }
    val currentSecond by remember(time) { mutableStateOf(time.second) }

    val displayHour = when {
        currentHour == 0 -> 12
        currentHour > 12 -> currentHour - 12
        else -> currentHour
    }

    val isAM = currentHour < 12

    val hours = (1..12).toList()
    val hourLabels = hours.map { it.toString() }

    val minutes = (0..59).toList()
    val minuteLabels = minutes.map { it.toString().padStart(2, '0') }

    val seconds = (0..59).toList()
    val secondLabels = seconds.map { it.toString().padStart(2, '0') }

    val amPmOptions = listOf("AM", "PM")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PickerWithPointerDragSupport(
            value = displayHour.toString(),
            values = hourLabels,
            onValueChange = { newHour ->
                val hour24 = when {
                    newHour.toInt() == 12 && isAM -> 0
                    newHour.toInt() == 12 && !isAM -> 12
                    !isAM -> newHour.toInt() + 12
                    else -> newHour.toInt()
                }
                onTimeChange(LocalTime(hour24, currentMinute, currentSecond))
            },
            modifier = Modifier.width(60.dp)
        )

        PickerWithPointerDragSupport(
            value = currentMinute.toString().padStart(2, '0'),
            values = minuteLabels,
            onValueChange = { newMinute ->
                onTimeChange(LocalTime(currentHour, newMinute.toInt(), currentSecond))
            },
            modifier = Modifier.width(60.dp)
        )

        if (showSecondsPicker) {
            PickerWithPointerDragSupport(
                value = currentSecond.toString().padStart(2, '0'),
                values = secondLabels,
                onValueChange = { newSecond ->
                    onTimeChange(LocalTime(currentHour, currentMinute, newSecond.toInt()))
                },
                modifier = Modifier.width(60.dp)
            )
        }

        PickerWithPointerDragSupport(
            value = if (isAM) "AM" else "PM",
            values = amPmOptions,
            onValueChange = { newAmPm ->
                val newHour24 = when {
                    displayHour == 12 && newAmPm == "AM" -> 0
                    displayHour == 12 && newAmPm == "PM" -> 12
                    newAmPm == "PM" -> displayHour + 12
                    else -> displayHour
                }
                onTimeChange(LocalTime(newHour24, currentMinute, currentSecond))
            },
            modifier = Modifier.width(60.dp)
        )
    }
}

@Composable
private fun PickerWithPointerDragSupport(
    value: String,
    values: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val numberOfVisibleItems = 5

    val textStyle = Theme[textStyles][title].copy(fontWeight = FontWeight.Normal)
    val itemHeight = with(LocalDensity.current) {
        (textStyle.fontSize.toDp() + 16.dp).coerceAtLeast(32.dp)
    }
    val listHeight = itemHeight * numberOfVisibleItems

    val currentValueChange by rememberUpdatedState(onValueChange)

    val lazyListState = rememberLazyListState()
    val itemHeightPx = with(LocalDensity.current) { itemHeight.roundToPx() }
    val flingBehavior = rememberSnapFlingBehavior(lazyListState)

    var focusedItemIndex by remember { mutableStateOf(-1) }

    val centeredFocusRequester = remember { FocusRequester() }

    val currentIndex by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex +
                    if (lazyListState.firstVisibleItemScrollOffset > itemHeightPx / 2) 1 else 0
        }
    }

    LaunchedEffect(Unit) {
        val index = values.indexOf(value).coerceAtLeast(0)
        lazyListState.scrollToItem(index)
        focusedItemIndex = index
    }

    LaunchedEffect(Unit) {
        snapshotFlow { currentIndex }
            .distinctUntilChanged()
            .collect { selectedIndex ->
                if (selectedIndex in values.indices) {
                    currentValueChange(values[selectedIndex])
                }
            }
    }

    LaunchedEffect(focusedItemIndex) {
        if (focusedItemIndex in values.indices) {
            lazyListState.animateScrollToItem(focusedItemIndex)
        }
    }

    val overscrollEffect = rememberOverscrollEffect()
    val pointerDragCoroutineScope = rememberCoroutineScope()

    LazyColumn(
        overscrollEffect = overscrollEffect?.withoutVisualEffect(),
        modifier = modifier
            .focusable(interactionSource = interactionSource)
            .overscroll(overscrollEffect)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        pointerDragCoroutineScope.launch {
                            lazyListState.animateScrollToItem(currentIndex)
                        }
                    },
                    onDragCancel = {
                        pointerDragCoroutineScope.launch {
                            lazyListState.animateScrollToItem(currentIndex)
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        pointerDragCoroutineScope.launch {
                            lazyListState.scrollBy(-dragAmount)
                        }
                    }
                )
            }
            .focusProperties {
                onEnter = {
                    centeredFocusRequester.requestFocus()
                }
            }
            .onKeyEvent { event ->
                when (event.key) {
                    Key.DirectionUp -> {
                        if (event.type == KeyEventType.KeyDown && focusedItemIndex > 0) {
                            val prevIndex = (focusedItemIndex - 1).coerceAtLeast(0)
                            focusedItemIndex = prevIndex
                        }
                        true
                    }

                    Key.DirectionDown -> {
                        if (event.type == KeyEventType.KeyDown && focusedItemIndex < values.lastIndex) {
                            val nextIndex = (focusedItemIndex + 1).coerceAtMost(values.size - 1)
                            focusedItemIndex = nextIndex
                        }
                        true
                    }

                    else -> false
                }
            }
            .focusRing(interactionSource, 2.dp, color = Theme[colors][focusRing], Theme[shapes][medium])
            .fadingEdge(Brush.verticalGradient(0f to Color.Transparent, 0.3f to Color.Black))
            .fadingEdge(Brush.verticalGradient(0.7f to Color.Black, 1f to Color.Transparent))
            .height(listHeight),
        state = lazyListState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = listHeight / 2 - itemHeight / 2),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(values.size) { index ->
            val alpha by animateFloatAsState(targetValue = if (index == currentIndex) 1f else 0.6f)
            Box(
                modifier = Modifier
                    .widthIn(min = 48.dp)
                    .alpha(alpha)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = values[index], style = textStyle)
            }
        }
    }
}

private fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }