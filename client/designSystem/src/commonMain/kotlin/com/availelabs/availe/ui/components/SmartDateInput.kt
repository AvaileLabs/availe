package com.availelabs.availe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.uikit.styling.*
import com.composeunstyled.UnstyledText
import com.composeunstyled.theme.Theme

@Composable
fun SmartDateInput(
    monthString: String,
    dayString: String,
    yearString: String,
    onMonthChanged: (String) -> Unit,
    onDayChanged: (String) -> Unit,
    onYearChanged: (String) -> Unit,
    monthPlaceholderString: String,
    dayPlaceholderString: String,
    yearPlaceholderString: String,
    hasError: Boolean,
    modifier: Modifier = Modifier
) {
    val monthFocusRequester = remember { FocusRequester() }
    val dayFocusRequester = remember { FocusRequester() }
    val yearFocusRequester = remember { FocusRequester() }

    val currentBorderColor = if (hasError) Theme[colors][destructive] else Theme[colors][outline].copy(alpha = 0.2f)

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(Theme[shapes][medium])
            .border(1.dp, currentBorderColor, Theme[shapes][medium])
            .background(Theme[colors][card])
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        SegmentedTextField(
            valueString = monthString,
            onValueChange = { newMonthString ->
                onMonthChanged(newMonthString)
                if (newMonthString.length == 2 && monthString.length < 2) {
                    dayFocusRequester.requestFocus()
                }
            },
            placeholderString = monthPlaceholderString,
            focusRequester = monthFocusRequester,
            onBackspaceOnEmpty = { },
            modifier = Modifier.width(28.dp)
        )

        UnstyledText(
            text = "/",
            color = Theme[colors][onBackground].copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        SegmentedTextField(
            valueString = dayString,
            onValueChange = { newDayString ->
                onDayChanged(newDayString)
                if (newDayString.length == 2 && dayString.length < 2) {
                    yearFocusRequester.requestFocus()
                }
            },
            placeholderString = dayPlaceholderString,
            focusRequester = dayFocusRequester,
            onBackspaceOnEmpty = { monthFocusRequester.requestFocus() },
            modifier = Modifier.width(28.dp)
        )

        UnstyledText(
            text = "/",
            color = Theme[colors][onBackground].copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        SegmentedTextField(
            valueString = yearString,
            onValueChange = { newYearString ->
                onYearChanged(newYearString)
            },
            placeholderString = yearPlaceholderString,
            focusRequester = yearFocusRequester,
            onBackspaceOnEmpty = { dayFocusRequester.requestFocus() },
            modifier = Modifier.width(42.dp)
        )
    }
}

@Composable
internal fun SegmentedTextField(
    valueString: String,
    onValueChange: (String) -> Unit,
    placeholderString: String,
    focusRequester: FocusRequester,
    onBackspaceOnEmpty: () -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = valueString,
        onValueChange = onValueChange,
        modifier = modifier
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                if (keyEvent.key == Key.Backspace && keyEvent.type == KeyEventType.KeyDown && valueString.isEmpty()) {
                    onBackspaceOnEmpty()
                    true
                } else {
                    false
                }
            },
        textStyle = Theme[textStyles][body].copy(
            color = Theme[colors][onBackground],
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (valueString.isEmpty()) {
                    UnstyledText(
                        text = placeholderString,
                        style = Theme[textStyles][body],
                        color = Theme[colors][onBackground].copy(alpha = 0.3f)
                    )
                }
                innerTextField()
            }
        },
        cursorBrush = SolidColor(Theme[colors][primary])
    )
}