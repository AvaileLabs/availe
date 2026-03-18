package com.availelabs.availe.ui.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.availelabs.availe.ui.components.dialogs.DimmedDialog
import com.composables.uikit.components.*
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme
import kotlinx.datetime.LocalTime

@Composable
fun TimePickerDialog(
    initialTime: LocalTime?,
    is24Hour: Boolean,
    onTimeSelected: (LocalTime) -> Unit,
    onDismissRequest: () -> Unit
) {
    DimmedDialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(Theme[shapes][small])
                .background(Theme[colors][card])
                .padding(20.dp)
        ) {
            var currentSelectedTime by remember { mutableStateOf(initialTime ?: LocalTime(12, 0)) }
            var currentIs24Hour by remember { mutableStateOf(is24Hour) }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select a time", style = Theme[textStyles][title])
                Spacer(Modifier.height(20.dp))

                TimePicker(
                    time = currentSelectedTime,
                    onTimeChange = { newTime -> currentSelectedTime = newTime },
                    use24HourFormat = currentIs24Hour,
                    showSecondsPicker = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = currentIs24Hour,
                        onCheckedChange = { isChecked -> currentIs24Hour = isChecked }
                    )
                    Text("Use 24 hours", style = Theme[textStyles][body])
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    PrimaryButton(
                        onClick = {
                            onTimeSelected(currentSelectedTime)
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set time")
                    }
                }
            }
        }
    }
}