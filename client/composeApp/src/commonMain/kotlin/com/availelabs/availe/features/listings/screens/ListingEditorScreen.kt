@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.listings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.features.listings.IListingEditorComponent
import com.availelabs.availe.features.listings.ListingType
import com.availelabs.availe.ui.ScreenScaffold
import com.availelabs.availe.ui.dismissKeyboardOnTap
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.*
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.destructive
import com.composables.uikit.styling.textStyles
import com.composables.uikit.styling.title
import com.composeunstyled.theme.Theme
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun ListingEditorScreen(component: IListingEditorComponent) {
    val state by component.state.subscribeAsState()

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .dismissKeyboardOnTap()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = component::onCancel) {
                    Icon(Lucide.ArrowLeft, "Back")
                }
                Text(
                    text = if (state.isEditing) "Edit Listing" else "Create Listing",
                    style = Theme[textStyles][title]
                )
            }

            Card(contentPadding = PaddingValues(16.dp)) {
                Text("Listing Type", color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListingType.entries.forEach { type ->
                        val isSelected = state.type == type
                        val isEnabled = !state.isEditing

                        if (isSelected) {
                            PrimaryButton(
                                onClick = { if (isEnabled) component.onTypeChanged(type) },
                                enabled = isEnabled
                            ) {
                                Text(type.name)
                            }
                        } else {
                            SecondaryButton(
                                onClick = { if (isEnabled) component.onTypeChanged(type) },
                                enabled = isEnabled
                            ) {
                                Text(type.name)
                            }
                        }
                    }
                }
            }

            TextField(
                value = state.title,
                onValueChange = component::onTitleChanged,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = state.description,
                onValueChange = component::onDescriptionChanged,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = state.priceAmount,
                    onValueChange = component::onPriceAmountChanged,
                    label = { Text("Price") },
                    modifier = Modifier.weight(1f)
                )

                Column(modifier = Modifier.width(100.dp)) {
                    Text("Currency", modifier = Modifier.padding(bottom = 8.dp))
                    if (state.priceCurrency == "USD") {
                        PrimaryButton(
                            onClick = { component.onCurrencyChanged("EUR") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("USD") }
                    } else {
                        SecondaryButton(
                            onClick = { component.onCurrencyChanged("USD") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("EUR") }
                    }
                }
            }

            TextField(
                value = state.imageUrl,
                onValueChange = component::onImageUrlChanged,
                label = { Text("Image URL") },
                leading = { Icon(Lucide.Image, null) },
                modifier = Modifier.fillMaxWidth()
            )

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = Theme[colors][destructive],
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)
            ) {
                SecondaryButton(
                    onClick = component::onCancel,
                    enabled = !state.isSubmitting
                ) {
                    Text("Cancel")
                }

                PrimaryButton(
                    onClick = component::onSubmit,
                    enabled = !state.isSubmitting
                ) {
                    if (state.isSubmitting) {
                        IntermediateProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text(if (state.isEditing) "Save Changes" else "Publish")
                    }
                }
            }
        }
    }
}