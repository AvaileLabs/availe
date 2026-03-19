package com.availelabs.availe.ui.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.*
import com.availelabs.availe.ui.components.dialogs.DimmedDialog
import com.composables.uikit.components.Checkbox
import com.composables.uikit.components.OutlinedButton
import com.composables.uikit.components.PrimaryButton
import com.composables.uikit.components.Text
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource

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
            var currentIsTwentyFourHourFormat by remember { mutableStateOf(is24Hour) }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(Res.string.dialog_time_picker_title),
                    style = Theme[textStyles][title]
                )
                Spacer(Modifier.height(20.dp))

                TimePickerWithPointerDragSupport(
                    time = currentSelectedTime,
                    onTimeChange = { newTime -> currentSelectedTime = newTime },
                    use24HourFormat = currentIsTwentyFourHourFormat,
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
                        checked = currentIsTwentyFourHourFormat,
                        onCheckedChange = { isChecked -> currentIsTwentyFourHourFormat = isChecked }
                    )
                    Text(
                        text = stringResource(Res.string.dialog_time_picker_twenty_four_hour),
                        style = Theme[textStyles][body]
                    )
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
                        Text(stringResource(Res.string.generic_cancel))
                    }
                    PrimaryButton(
                        onClick = {
                            onTimeSelected(currentSelectedTime)
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.dialog_time_picker_confirm))
                    }
                }
            }
        }
    }
}