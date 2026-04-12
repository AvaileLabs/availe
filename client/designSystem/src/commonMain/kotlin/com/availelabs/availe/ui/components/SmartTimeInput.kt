package com.availelabs.availe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.uikit.styling.*
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme

@Composable
fun SmartTimeInput(
    hourString: String,
    minuteString: String,
    meridiemIndicatorString: String,
    isTwentyFourHourFormat: Boolean,
    onHourChanged: (String) -> Unit,
    onMinuteChanged: (String) -> Unit,
    onMeridiemToggled: () -> Unit,
    hourPlaceholderString: String,
    minutePlaceholderString: String,
    hasError: Boolean,
    modifier: Modifier = Modifier
) {
    val hourFocusRequester = remember { FocusRequester() }
    val minuteFocusRequester = remember { FocusRequester() }

    val currentBorderColor = if (hasError) Theme[colors][destructive] else Theme[colors][outline].copy(alpha = 0.2f)

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(Theme[shapes][medium])
            .border(1.dp, currentBorderColor, Theme[shapes][medium])
            .background(Theme[colors][card])
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        SegmentedTextField(
            valueString = hourString,
            onValueChange = { newHourString ->
                onHourChanged(newHourString)
                if (newHourString.length == 2 && hourString.length < 2) {
                    minuteFocusRequester.requestFocus()
                }
            },
            placeholderString = hourPlaceholderString,
            focusRequester = hourFocusRequester,
            onBackspaceOnEmpty = { },
            modifier = Modifier.width(28.dp)
        )

        Text(
            text = ":",
            color = Theme[colors][onBackground].copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        SegmentedTextField(
            valueString = minuteString,
            onValueChange = { newMinuteString ->
                onMinuteChanged(newMinuteString)
            },
            placeholderString = minutePlaceholderString,
            focusRequester = minuteFocusRequester,
            onBackspaceOnEmpty = { hourFocusRequester.requestFocus() },
            modifier = Modifier.width(28.dp)
        )

        if (!isTwentyFourHourFormat) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(Theme[shapes][small])
                    .background(Theme[colors][outline].copy(alpha = 0.1f))
                    .clickable(onClick = onMeridiemToggled)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = meridiemIndicatorString,
                    style = Theme[textStyles][caption].copy(fontWeight = FontWeight.Bold),
                    color = Theme[colors][onBackground]
                )
            }
        }
    }
}