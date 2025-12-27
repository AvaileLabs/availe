package com.availelabs.availe.features.listingeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.availelabs.availe.ListingEditorIntent
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.features.listingeditor.IListingEditorComponent
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.*
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.destructive
import com.composables.uikit.styling.outline
import com.composables.uikit.styling.shapes
import com.composeunstyled.DropdownPanelAnchor
import com.composeunstyled.theme.Theme

@Composable
fun ListingEditorScreen(
    component: IListingEditorComponent
) {
    val state by component.state.collectAsState()
    val scrollState = rememberScrollState()

    AppScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = state.title.value,
                        onValueChange = { component.onIntent(ListingEditorIntent.ChangeTitle(ListingTitle(it))) },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TextField(
                            value = state.priceAmount.value,
                            onValueChange = { component.onIntent(ListingEditorIntent.ChangePrice(PriceAmount(it))) },
                            label = { Text("Price") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        CurrencyDropdown(
                            selectedCurrency = state.priceCurrency,
                            onCurrencySelected = { component.onIntent(ListingEditorIntent.ChangeCurrency(it)) }
                        )
                    }
                }

                Card(modifier = Modifier.fillMaxWidth(), title = { Text("Category") }) {
                    RadioGroup(
                        value = state.type.name,
                        onValueChange = {
                            val category = ListingCategory.valueOf(it)
                            component.onIntent(ListingEditorIntent.ChangeType(category))
                        }
                    ) {
                        ListingCategory.entries.forEach { category ->
                            RadioButton(
                                option = category.name,
                                selected = state.type == category
                            ) {
                                Text(category.name)
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = state.description.value,
                        onValueChange = { component.onIntent(ListingEditorIntent.ChangeDescription(ListingDescription(it))) },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        singleLine = false
                    )
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = state.imageUrl.value,
                        onValueChange = { component.onIntent(ListingEditorIntent.ChangeImage(ImageUrl(it))) },
                        label = { Text("Image URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.imageUrl.value.isNotBlank()) {
                        AsyncImage(
                            model = state.imageUrl.value,
                            contentDescription = "Listing Image Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(Theme[shapes][com.composables.uikit.styling.medium]),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(Theme[shapes][com.composables.uikit.styling.medium])
                                .background(Theme[colors][outline].copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Lucide.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Theme[colors][outline]
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No image preview",
                                    color = Theme[colors][outline]
                                )
                            }
                        }
                    }
                }

                if (state.error != null) {
                    Text(
                        text = state.error!!,
                        color = Theme[colors][destructive],
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Extra spacer for bottom bar / FAB area
                Spacer(modifier = Modifier.height(80.dp))
            }

            if (state.isSubmitting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Theme[colors][com.composables.uikit.styling.scrim])
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    IntermediateProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun CurrencyDropdown(
    selectedCurrency: SupportedCurrency,
    onCurrencySelected: (SupportedCurrency) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    DropdownMenu(
        onExpandRequest = { expanded = true },
        modifier = Modifier.width(120.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp) // Align visually with text field label offset
                .height(56.dp)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "${selectedCurrency.code.value} (${selectedCurrency.symbol})")
            Icon(Lucide.ChevronDown, null, modifier = Modifier.size(16.dp))
        }

        DropdownMenuPanel(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            anchor = DropdownPanelAnchor.BottomStart
        ) {
            SupportedCurrency.entries.forEach { currency ->
                DropdownMenuItem(
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                    }
                ) {
                    Text(text = "${currency.code.value} (${currency.symbol})")
                }
            }
        }
    }
}