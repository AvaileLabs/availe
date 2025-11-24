@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.listings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.domain.ListingEditorIntent
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.features.listings.IListingEditorComponent
import com.availelabs.availe.ui.ScreenScaffold
import com.availelabs.availe.ui.components.DismissibleFormField
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
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .dismissKeyboardOnTap()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
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

            Spacer(
                Modifier
                    .height(16.dp)
                    .fillMaxWidth()
                    .clickable(interactionSource = interactionSource, indication = null) {
                        focusManager.clearFocus()
                    }
            )

            Card(contentPadding = PaddingValues(16.dp)) {
                Text("Listing Type", color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListingCategory.entries.forEach { type ->
                        val isSelected = state.type == type
                        val isEnabled = !state.isEditing

                        if (isSelected) {
                            PrimaryButton(
                                onClick = {
                                    if (isEnabled) component.onIntent(ListingEditorIntent.ChangeType(type))
                                },
                                enabled = isEnabled
                            ) {
                                Text(type.name)
                            }
                        } else {
                            SecondaryButton(
                                onClick = {
                                    if (isEnabled) component.onIntent(ListingEditorIntent.ChangeType(type))
                                },
                                enabled = isEnabled
                            ) {
                                Text(type.name)
                            }
                        }
                    }
                }
            }

            Spacer(
                Modifier
                    .height(16.dp)
                    .fillMaxWidth()
                    .clickable(interactionSource = interactionSource, indication = null) {
                        focusManager.clearFocus()
                    }
            )

            DismissibleFormField(label = "Title") {
                TextField(
                    value = state.title.value,
                    onValueChange = { component.onIntent(ListingEditorIntent.ChangeTitle(ListingTitle(it))) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            DismissibleFormField(label = "Description") {
                TextField(
                    value = state.description.value,
                    onValueChange = { component.onIntent(ListingEditorIntent.ChangeDescription(ListingDescription(it))) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DismissibleFormField(
                    label = "Price",
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = state.priceAmount.value,
                        onValueChange = { component.onIntent(ListingEditorIntent.ChangePrice(PriceAmount(it))) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.width(100.dp)) {
                    Text(
                        "Currency",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = interactionSource, indication = null) {
                                focusManager.clearFocus()
                            }
                            .padding(bottom = 8.dp)
                    )

                    SupportedCurrency.entries.forEach { currency ->
                        val isSelected = state.priceCurrency == currency
                        if (isSelected) {
                            PrimaryButton(
                                onClick = { },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(currency.code) }
                        } else {
                            SecondaryButton(
                                onClick = { component.onIntent(ListingEditorIntent.ChangeCurrency(currency)) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(currency.code) }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            DismissibleFormField(label = "Image URL") {
                TextField(
                    value = state.imageUrl.value,
                    onValueChange = { component.onIntent(ListingEditorIntent.ChangeImage(ImageUrl(it))) },
                    leading = { Icon(Lucide.Image, null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

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
                    onClick = { component.onIntent(ListingEditorIntent.Submit) },
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