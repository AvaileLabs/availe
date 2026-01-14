package com.availelabs.availe.listingeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
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
import com.availelabs.availe.ListingEditorState
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.ui.components.dialogs.DestructiveActionModal
import com.availelabs.availe.ui.layout.WindowSizeClass
import com.availelabs.availe.ui.layout.rememberWindowSize
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.*
import com.composables.uikit.styling.*
import com.composeunstyled.DropdownPanelAnchor
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme

@Composable
fun ListingEditorScreen(
    component: IListingEditorComponent
) {
    val state by component.state.collectAsState()
    val wizardState by component.wizardState.collectAsState()
    val windowSize = rememberWindowSize()

    AppScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Theme[colors][background])
        ) {
            val isMultiPane = windowSize !is WindowSizeClass.Compact
            if (isMultiPane) {
                Row(modifier = Modifier.fillMaxSize()) {
                    ListingEditorWizardStepList(
                        editorState = state,
                        wizardState = wizardState,
                        onStepClicked = { component.onWizardIntent(ListingEditorWizardIntent.NavigateTo(it)) },
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .padding(16.dp)
                    )

                    ListingEditorWizardContent(
                        editorState = state,
                        wizardState = wizardState,
                        onEditorIntent = component::onIntent,
                        onWizardIntent = component::onWizardIntent,
                        shouldShowProgressBar = false,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(16.dp)
                    )
                }
            } else {
                ListingEditorWizardContent(
                    editorState = state,
                    wizardState = wizardState,
                    onEditorIntent = component::onIntent,
                    onWizardIntent = component::onWizardIntent,
                    shouldShowProgressBar = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }

            if (state.isSubmitting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Theme[colors][scrim])
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    IntermediateProgressIndicator()
                }
            }

            if (state.showClearConfirmationDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Theme[colors][scrim])
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    DestructiveActionModal(
                        title = "Clear all changes?",
                        message = "This will remove all details you have entered. This action cannot be undone.",
                        confirmText = "Clear",
                        onConfirm = { component.onIntent(ListingEditorIntent.ConfirmClear) },
                        onDismiss = { component.onIntent(ListingEditorIntent.CancelClear) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ListingEditorWizardContent(
    editorState: ListingEditorState,
    wizardState: ListingEditorWizardState,
    onEditorIntent: (ListingEditorIntent) -> Unit,
    onWizardIntent: (ListingEditorWizardIntent) -> Unit,
    shouldShowProgressBar: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val currentErrorMessage = getBlockingStepErrorMessage(wizardState, editorState)

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (shouldShowProgressBar) {
                ListingEditorSegmentedProgressBar(
                    editorState = editorState,
                    wizardState = wizardState,
                    onStepClicked = { onWizardIntent(ListingEditorWizardIntent.NavigateTo(it)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SelectionContainer {
                Text(
                    text = "${wizardState.currentStep.title} (${wizardState.currentStepIndex + 1} of ${wizardState.steps.size})"
                )
            }

            when (wizardState.currentStep) {
                ListingEditorWizardStep.ChooseType -> ListingEditorChooseTypeStep(
                    selectedCategory = wizardState.selectedCategory,
                    onCategorySelected = { onWizardIntent(ListingEditorWizardIntent.SelectType(it)) }
                )

                ListingEditorWizardStep.Basics -> ListingEditorBasicsStep(
                    title = editorState.title,
                    description = editorState.description,
                    onTitleChanged = { onEditorIntent(ListingEditorIntent.ChangeTitle(it)) },
                    onDescriptionChanged = { onEditorIntent(ListingEditorIntent.ChangeDescription(it)) }
                )

                ListingEditorWizardStep.Pricing -> ListingEditorPricingStep(
                    priceAmount = editorState.priceAmount,
                    priceCurrency = editorState.priceCurrency,
                    onPriceChanged = { onEditorIntent(ListingEditorIntent.ChangePrice(it)) },
                    onCurrencySelected = { onEditorIntent(ListingEditorIntent.ChangeCurrency(it)) }
                )

                ListingEditorWizardStep.Media -> ListingEditorMediaStep(
                    imageUrl = editorState.imageUrl,
                    onImageUrlChanged = { onEditorIntent(ListingEditorIntent.ChangeImage(it)) }
                )

                ListingEditorWizardStep.Review -> ListingEditorReviewStep(
                    editorState = editorState,
                    isEditingFlow = wizardState.isEditingFlow,
                    selectedCategory = wizardState.selectedCategory
                )
            }

            if (editorState.error != null) {
                SelectionContainer {
                    Text(
                        text = editorState.error,
                        color = Theme[colors][destructive]
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Theme[colors][background])
                .padding(top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isBackVisible = wizardState.currentStepIndex > 0
            val isNextEnabled = currentErrorMessage == null
            val primaryLabel = if (wizardState.currentStep == ListingEditorWizardStep.Review) {
                if (wizardState.isEditingFlow) "Save" else "Publish"
            } else {
                "Next"
            }

            if (isBackVisible) {
                OutlinedButton(
                    onClick = { onWizardIntent(ListingEditorWizardIntent.Back) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            PrimaryButton(
                onClick = { onWizardIntent(ListingEditorWizardIntent.Next) },
                enabled = isNextEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(primaryLabel)
            }
        }
    }
}

@Composable
private fun ListingEditorChooseTypeStep(
    selectedCategory: ListingCategory?,
    onCategorySelected: (ListingCategory) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), title = {
        SelectionContainer {
            Text("Listing type")
        }
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ListingEditorTypeOption(
                title = "Item",
                isSelected = selectedCategory == ListingCategory.Item,
                onClick = { onCategorySelected(ListingCategory.Item) }
            )
            ListingEditorTypeOption(
                title = "Event",
                isSelected = selectedCategory == ListingCategory.Event,
                onClick = { onCategorySelected(ListingCategory.Event) }
            )
            ListingEditorTypeOption(
                title = "Service",
                isSelected = selectedCategory == ListingCategory.Service,
                onClick = { onCategorySelected(ListingCategory.Service) }
            )
        }
    }
}

@Composable
private fun ListingEditorTypeOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme[shapes][medium])
            .background(
                if (isSelected) Theme[colors][outline].copy(alpha = 0.12f) else Theme[colors][background]
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title)
    }
}

@Composable
private fun ListingEditorBasicsStep(
    title: ListingTitle,
    description: ListingDescription,
    onTitleChanged: (ListingTitle) -> Unit,
    onDescriptionChanged: (ListingDescription) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = title.value,
            onValueChange = { onTitleChanged(ListingTitle(it)) },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = description.value,
            onValueChange = { onDescriptionChanged(ListingDescription(it)) },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            singleLine = false
        )
    }
}

@Composable
private fun ListingEditorPricingStep(
    priceAmount: PriceAmount,
    priceCurrency: SupportedCurrency,
    onPriceChanged: (PriceAmount) -> Unit,
    onCurrencySelected: (SupportedCurrency) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = priceAmount.value,
                onValueChange = { onPriceChanged(PriceAmount(it)) },
                label = { Text("Price") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            CurrencyDropdown(
                selectedCurrency = priceCurrency,
                onCurrencySelected = onCurrencySelected
            )
        }
    }
}

@Composable
private fun ListingEditorMediaStep(
    imageUrl: ImageUrl,
    onImageUrlChanged: (ImageUrl) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = imageUrl.value,
            onValueChange = { onImageUrlChanged(ImageUrl(it)) },
            label = { Text("Image URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (imageUrl.value.isNotBlank()) {
            AsyncImage(
                model = imageUrl.value,
                contentDescription = "Listing Image Preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(Theme[shapes][medium]),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(Theme[shapes][medium])
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
                    SelectionContainer {
                        Text(
                            text = "No image preview",
                            color = Theme[colors][outline]
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListingEditorReviewStep(
    editorState: ListingEditorState,
    isEditingFlow: Boolean,
    selectedCategory: ListingCategory?
) {
    val categoryText = when {
        isEditingFlow -> editorState.type.name
        selectedCategory != null -> selectedCategory.name
        else -> ""
    }

    Card(modifier = Modifier.fillMaxWidth(), title = {
        SelectionContainer {
            Text("Review")
        }
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (categoryText.isNotBlank()) {
                SelectionContainer {
                    Text("Type: $categoryText")
                }
            }
            SelectionContainer {
                Text("Title: ${editorState.title.value}")
            }
            SelectionContainer {
                Text("Price: ${editorState.priceCurrency.symbol}${editorState.priceAmount.value}")
            }
            if (editorState.description.value.isNotBlank()) {
                SelectionContainer {
                    Text("Description: ${editorState.description.value}")
                }
            }
            if (editorState.imageUrl.value.isNotBlank()) {
                SelectionContainer {
                    Text("Image URL: ${editorState.imageUrl.value}")
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