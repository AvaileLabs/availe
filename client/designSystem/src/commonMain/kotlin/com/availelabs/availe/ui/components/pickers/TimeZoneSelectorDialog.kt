package com.availelabs.availe.ui.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.availelabs.availe.ui.components.dialogs.DimmedDialog
import com.composables.uikit.components.OutlinedButton
import com.composables.uikit.components.Text
import com.composables.uikit.components.TextField
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme
import kotlinx.datetime.TimeZone

@Composable
fun TimeZoneSelectorDialog(
    initialZoneId: String,
    onZoneSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val availableTimeZones = remember { TimeZone.availableZoneIds.toList().sorted() }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTimeZones = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            availableTimeZones
        } else {
            val lowercaseQuery = searchQuery.lowercase()
            availableTimeZones.filter { timeZoneId ->
                timeZoneId.lowercase().contains(lowercaseQuery) || formatTimeZoneForDisplay(timeZoneId).lowercase()
                    .contains(lowercaseQuery)
            }
        }
    }

    DimmedDialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .clip(Theme[shapes][small])
                .background(Theme[colors][card])
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select Time Zone", style = Theme[textStyles][title])
                Spacer(Modifier.height(16.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { newQuery -> searchQuery = newQuery },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(filteredTimeZones) { timeZoneIdentifierString ->
                        val isSelected = timeZoneIdentifierString == initialZoneId
                        val backgroundColor =
                            if (isSelected) Theme[colors][outline].copy(alpha = 0.1f) else Color.Transparent

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Theme[shapes][small])
                                .background(backgroundColor)
                                .clickable {
                                    onZoneSelected(timeZoneIdentifierString)
                                    onDismissRequest()
                                }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = formatTimeZoneForDisplay(timeZoneIdentifierString),
                                style = Theme[textStyles][body],
                                color = if (isSelected) Theme[colors][primary] else Theme[colors][onCard]
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

private fun formatTimeZoneForDisplay(timeZoneIdentifierString: String): String {
    val parts = timeZoneIdentifierString.split("/")
    if (parts.size < 2) return timeZoneIdentifierString.replace("_", " ")

    val region = parts[0]
    val city = parts.drop(1).joinToString(" / ") { it.replace("_", " ") }

    return "$city ($region)"
}