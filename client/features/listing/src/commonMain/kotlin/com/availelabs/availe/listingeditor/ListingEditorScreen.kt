package com.availelabs.availe.listingeditor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import com.availelabs.availe.ListingEditorState
import com.availelabs.availe.domain.extensions.isValid
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.ui.components.dialogs.DestructiveActionModal
import com.availelabs.availe.ui.components.sheets.ListingStepsBottomSheet
import com.availelabs.availe.ui.layout.WindowSizeClass
import com.availelabs.availe.ui.layout.rememberWindowSize
import com.availelabs.availe.ui.util.rememberSyncedPagerState
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.CloudUpload
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
    val state by component.state.subscribeAsState()
    val pages by component.pages.subscribeAsState()
    val windowSize = rememberWindowSize()

    AppScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Theme[colors][background])
        ) {
            val isMultiPane = windowSize !is WindowSizeClass.Compact

            if (isMultiPane) {
                // Desktop / Tablet Layout
                Row(modifier = Modifier.fillMaxSize()) {
                    ListingEditorWizardStepList(
                        pages = pages,
                        editorState = state,
                        onStepClicked = component::onPageSelected,
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .padding(16.dp)
                    )

                    ListingEditorWizardContent(
                        component = component,
                        state = state,
                        pages = component.pages,
                        shouldShowProgressBar = false,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(16.dp)
                    )
                }
            } else {
                // Mobile Layout
                ListingEditorWizardContent(
                    component = component,
                    state = state,
                    pages = component.pages,
                    shouldShowProgressBar = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }

            // Global Loading Indicator
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

            // Modals
            if (state.showDiscardDraftDialog) {
                DestructiveActionModal(
                    title = "Discard Draft?",
                    message = "Are you sure you want to discard this draft? All progress will be lost.",
                    confirmText = "Discard",
                    onConfirm = component::onConfirmDiscardDraft,
                    onDismiss = component::onCancelDiscardDraft
                )
            }

            if (state.showPublishConfirmationDialog) {
                PublishConfirmationDialog(
                    onConfirm = component::onConfirmPublish,
                    onDismiss = component::onCancelPublish
                )
            }
        }
    }
}

@Composable
private fun ListingEditorWizardContent(
    component: IListingEditorComponent,
    state: ListingEditorState,
    pages: Value<ChildPages<ListingEditorPageConfig, ListingEditorPage>>,
    shouldShowProgressBar: Boolean,
    modifier: Modifier = Modifier
) {
    val pagesState by pages.subscribeAsState()
    val scrollState = rememberScrollState()
    val stepNames = listOf("Type", "Basics", "Pricing", "Media", "Review")
    val currentStepIndex = pagesState.selectedIndex
    val currentStepName = stepNames.getOrElse(currentStepIndex) { "" }
    var showStepsOverlay by remember { mutableStateOf(false) }

    // Validation logic for "Next" button
    val isNextEnabled = remember(state, currentStepIndex) {
        val config = pagesState.items[currentStepIndex].configuration
        when (config) {
            ListingEditorPageConfig.Type -> true
            ListingEditorPageConfig.Basics -> state.title.isValid()
            ListingEditorPageConfig.Pricing -> state.priceAmount.isValid() && state.priceAmount.value.isNotBlank()
            ListingEditorPageConfig.Media -> true
            ListingEditorPageConfig.Review -> state.title.isValid() && state.priceAmount.value.isNotBlank()
        }
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header: Progress Bar with Long Press Interaction
            if (shouldShowProgressBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Combined clickable for Long Press interaction
                        .combinedClickable(
                            onClick = { /* No-op or toast "Long press to see steps" */ },
                            onLongClick = { showStepsOverlay = true }
                        )
                        .padding(vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ListingEditorSegmentedProgressBar(
                            pages = pagesState,
                            editorState = state,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Text label below progress bar to indicate context
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$currentStepName (${currentStepIndex + 1} of ${stepNames.size})",
                                style = Theme[textStyles][body],
                                fontWeight = FontWeight.Bold,
                                color = Theme[colors][onBackground].copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Lucide.ChevronDown, null, Modifier.size(14.dp))
                        }
                    }
                }
            } else {
                // Desktop Header Title
                SelectionContainer {
                    Text(
                        text = "$currentStepName (${currentStepIndex + 1} of ${stepNames.size})",
                        style = Theme[textStyles][title]
                    )
                }
            }

            // Form Content
            val pagerState = rememberSyncedPagerState(
                pagesValue = pages,
                onPageSelected = component::onPageSelected
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                userScrollEnabled = false,
                verticalAlignment = Alignment.Top
            ) { pageIndex ->
                when (pagesState.items[pageIndex].configuration) {
                    ListingEditorPageConfig.Type -> ListingEditorChooseTypeStep(
                        selectedCategory = state.type,
                        onCategorySelected = component::onTypeSelected
                    )

                    ListingEditorPageConfig.Basics -> ListingEditorBasicsStep(
                        title = state.title,
                        description = state.description,
                        onTitleChanged = component::onTitleChanged,
                        onDescriptionChanged = component::onDescriptionChanged
                    )

                    ListingEditorPageConfig.Pricing -> ListingEditorPricingStep(
                        priceAmount = state.priceAmount,
                        priceCurrency = state.priceCurrency,
                        onPriceChanged = component::onPriceAmountChanged,
                        onCurrencySelected = component::onCurrencySelected
                    )

                    ListingEditorPageConfig.Media -> ListingEditorMediaStep(
                        imageUrl = state.imageUrl,
                        onImageUrlChanged = component::onImageUrlChanged
                    )

                    ListingEditorPageConfig.Review -> ListingEditorReviewStep(
                        editorState = state
                    )
                }
            }

            if (state.error != null) {
                SelectionContainer {
                    Text(
                        text = state.error,
                        color = Theme[colors][destructive]
                    )
                }
            }
        }

        // Bottom Bar (Back / Next / Publish)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Theme[colors][background])
                .padding(top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isBackVisible = currentStepIndex > 0
            val primaryLabel = if (currentStepIndex == stepNames.lastIndex) "Publish" else "Next"

            if (isBackVisible) {
                OutlinedButton(
                    onClick = { component.onPageSelected(currentStepIndex - 1) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            PrimaryButton(
                onClick = {
                    if (currentStepIndex < stepNames.lastIndex) {
                        component.onPageSelected(currentStepIndex + 1)
                    } else {
                        component.onPublishClicked()
                    }
                },
                enabled = isNextEnabled && !state.isSubmitting,
                modifier = Modifier.weight(1f)
            ) {
                Text(primaryLabel)
            }
        }

        // Bottom Sheet Overlay
        if (showStepsOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showStepsOverlay = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                ListingStepsBottomSheet(
                    currentStepIndex = currentStepIndex,
                    steps = stepNames,
                    onStepSelected = {
                        component.onPageSelected(it)
                        showStepsOverlay = false
                    },
                    modifier = Modifier.clickable(enabled = false) {}
                )
            }
        }
    }
}

// --- Restored Step Composables ---

@Composable
private fun ListingEditorChooseTypeStep(
    selectedCategory: ListingCategory,
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
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = title.value,
            onValueChange = onTitleChanged,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = description.value,
            onValueChange = onDescriptionChanged,
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
    onPriceChanged: (String) -> Unit,
    onCurrencySelected: (SupportedCurrency) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = priceAmount.value,
                onValueChange = onPriceChanged,
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
private fun CurrencyDropdown(
    selectedCurrency: SupportedCurrency,
    onCurrencySelected: (SupportedCurrency) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .width(120.dp)
                .padding(top = 8.dp)
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

@Composable
private fun ListingEditorMediaStep(
    imageUrl: ImageUrl,
    onImageUrlChanged: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = imageUrl.value,
            onValueChange = onImageUrlChanged,
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
    editorState: ListingEditorState
) {
    Card(modifier = Modifier.fillMaxWidth(), title = {
        SelectionContainer {
            Text("Review")
        }
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectionContainer {
                Text("Type: ${editorState.type.name}")
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
private fun PublishConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(320.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Theme[shadows][modal].color
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Theme[colors][card])
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Theme[colors][primary].copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Theme[colors][primary]
                )
            }

            Spacer(Modifier.height(16.dp))

            SelectionContainer {
                Text(
                    text = "Publish Listing?",
                    style = Theme[textStyles][title],
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(8.dp))

            SelectionContainer {
                Text(
                    text = "Your listing will be visible to everyone. You can edit it later.",
                    style = Theme[textStyles][body].copy(
                        color = Theme[colors][onBackground].copy(alpha = 0.7f)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    backgroundColor = Theme[colors][card],
                    contentColor = Theme[colors][onBackground],
                    borderWidth = 1.dp,
                    borderColor = Theme[colors][outline].copy(alpha = 0.2f)
                ) {
                    Text("Cancel")
                }

                PrimaryButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Publish")
                }
            }
        }
    }
}