package com.availelabs.availe.listingeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import availe.client.designsystem.generated.resources.*
import coil3.compose.AsyncImage
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import com.availelabs.availe.ListingEditorState
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.ui.components.dialogs.ActionConfirmationModal
import com.availelabs.availe.ui.layout.WindowSizeClass
import com.availelabs.availe.ui.layout.rememberWindowSize
import com.availelabs.availe.ui.model.asString
import com.availelabs.availe.ui.util.rememberSyncedPagerState
import com.composables.core.SheetDetent
import com.composables.core.rememberModalBottomSheetState
import com.composables.icons.lucide.*
import com.composables.uikit.components.*
import com.composables.uikit.styling.*
import com.composeunstyled.DropdownPanelAnchor
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun ListingEditorScreen(
    component: IListingEditorComponent
) {
    val state by component.state.subscribeAsState()
    val pages by component.pages.subscribeAsState()
    val windowSize = rememberWindowSize()

    val shouldShowProgressBar = windowSize is WindowSizeClass.Compact
    val sheetState = rememberModalBottomSheetState(initialDetent = SheetDetent.Hidden)
    val scope = rememberCoroutineScope()

    val currentStepIndex = pages.selectedIndex
    val currentConfig = pages.items[currentStepIndex].configuration
    val currentStepName = stringResource(currentConfig.titleRes)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme[colors][background])
    ) {
        AppScaffold {
            Box(modifier = Modifier.fillMaxSize()) {
                val isMultiPane = windowSize !is WindowSizeClass.Compact

                if (isMultiPane) {
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
                            onOpenSteps = {},
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(16.dp)
                        )
                    }
                } else {
                    ListingEditorWizardContent(
                        component = component,
                        state = state,
                        pages = component.pages,
                        shouldShowProgressBar = true,
                        onOpenSteps = {
                            scope.launch { sheetState.targetDetent = SheetDetent.FullyExpanded }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
        }

        if (shouldShowProgressBar) {
            ModalBottomSheet(state = sheetState) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Theme[colors][card])
                ) {
                    ListingStepsBottomSheet(
                        pages = pages,
                        editorState = state,
                        onStepSelected = {
                            component.onPageSelected(it)
                            scope.launch { sheetState.targetDetent = SheetDetent.Hidden }
                        }
                    )
                }
            }
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

        if (state.showDiscardDraftDialog) {
            DimmedDialog(onDismissRequest = component::onCancelDiscardDraft) {
                DraftOptionsDialog(
                    stepName = currentStepName,
                    onClearStep = { component.onClearStep(currentConfig) },
                    onRequestDeleteDraft = component::onRequestDeleteDraft,
                    onDismiss = component::onCancelDiscardDraft
                )
            }
        }

        if (state.showDeleteDraftConfirmation) {
            DimmedDialog(onDismissRequest = component::onCancelDeleteDraft) {
                ActionConfirmationModal(
                    icon = Lucide.TriangleAlert,
                    iconContentColor = Theme[colors][destructive],
                    iconBackgroundColor = Theme[colors][destructive].copy(alpha = 0.1f),
                    title = stringResource(Res.string.listing_editor_delete_confirm_title),
                    message = stringResource(Res.string.listing_editor_delete_confirm_body),
                    confirmText = stringResource(Res.string.generic_delete),
                    cancelText = stringResource(Res.string.generic_cancel),
                    onConfirm = component::onConfirmDiscardDraft,
                    onDismiss = component::onCancelDeleteDraft,
                    confirmButtonColor = Theme[colors][destructive],
                    confirmButtonContentColor = Theme[colors][onPrimary]
                )
            }
        }

        if (state.showPublishConfirmationDialog) {
            DimmedDialog(onDismissRequest = component::onCancelPublish) {
                ActionConfirmationModal(
                    icon = Lucide.CloudUpload,
                    iconContentColor = Theme[colors][primary],
                    iconBackgroundColor = Theme[colors][primary].copy(alpha = 0.1f),
                    title = stringResource(Res.string.listing_editor_publish_confirm_title),
                    message = stringResource(Res.string.listing_editor_publish_confirm_body),
                    confirmText = stringResource(Res.string.listing_editor_action_publish),
                    cancelText = stringResource(Res.string.generic_cancel),
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
    onOpenSteps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagesState by pages.subscribeAsState()
    val scrollState = rememberScrollState()
    val currentStepIndex = pagesState.selectedIndex
    val currentStepName = stringResource(pagesState.items[currentStepIndex].configuration.titleRes)
    val totalSteps = pagesState.items.size

    val isNextEnabled = remember(state, currentStepIndex) {
        val config = pagesState.items[currentStepIndex].configuration
        getStepValidationStatus(config, state)
    }

    val swipeUpConnection = remember(shouldShowProgressBar, onOpenSteps) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (shouldShowProgressBar && source == NestedScrollSource.UserInput && available.y < -20f) {
                    onOpenSteps()
                    return available
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                if (shouldShowProgressBar && available.y < -1000f) {
                    onOpenSteps()
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Box(modifier = modifier.nestedScroll(swipeUpConnection)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (shouldShowProgressBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSteps() }
                        .padding(vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ListingEditorSegmentedProgressBar(
                            pages = pagesState,
                            editorState = state,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(
                                    Res.string.listing_editor_step_progress_format,
                                    currentStepName,
                                    currentStepIndex + 1,
                                    totalSteps
                                ),
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
                SelectionContainer {
                    Text(
                        text = stringResource(
                            Res.string.listing_editor_step_progress_format,
                            currentStepName,
                            currentStepIndex + 1,
                            totalSteps
                        ),
                        style = Theme[textStyles][title]
                    )
                }
            }

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
                        text = mapListingEditorError(state.error).asString(),
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
            val isBackVisible = currentStepIndex > 0
            val primaryLabel = if (currentStepIndex == totalSteps - 1)
                stringResource(Res.string.listing_editor_action_publish)
            else
                stringResource(Res.string.listing_editor_action_next)

            if (isBackVisible) {
                OutlinedButton(
                    onClick = { component.onPageSelected(currentStepIndex - 1) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.generic_back))
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            PrimaryButton(
                onClick = {
                    if (currentStepIndex < totalSteps - 1) {
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
    }
}

@Composable
private fun DraftOptionsDialog(
    stepName: String,
    onClearStep: () -> Unit,
    onRequestDeleteDraft: () -> Unit,
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
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.generic_options),
                    style = Theme[textStyles][title],
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(35.dp)) {
                    Icon(Lucide.X, null, tint = Theme[colors][onBackground].copy(alpha = 0.5f))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Theme[colors][outline].copy(alpha = 0.1f))
            )

            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                DropdownMenuItem(onClick = onClearStep) {
                    Icon(Lucide.Eraser, null, Modifier.size(20.dp))
                    Text(stringResource(Res.string.listing_editor_draft_action_clear_step_format, stepName))
                }

                DropdownMenuItem(onClick = onRequestDeleteDraft) {
                    Icon(Lucide.Trash2, null, Modifier.size(20.dp), tint = Theme[colors][destructive])
                    Text(
                        text = stringResource(Res.string.listing_editor_draft_action_delete),
                        color = Theme[colors][destructive],
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun DimmedDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(enabled = true, onClick = onDismissRequest),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.clickable(enabled = false) {}) {
                content()
            }
        }
    }
}

@Composable
private fun ListingEditorChooseTypeStep(
    selectedCategory: ListingCategory,
    onCategorySelected: (ListingCategory) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), title = {
        SelectionContainer {
            Text(stringResource(Res.string.listing_editor_label_type))
        }
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ListingCategory.entries.forEach { category ->
                ListingEditorTypeOption(
                    title = stringResource(category.titleRes),
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }
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
            label = { Text(stringResource(Res.string.listing_editor_label_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = description.value,
            onValueChange = onDescriptionChanged,
            label = { Text(stringResource(Res.string.listing_editor_label_description)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
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
                label = { Text(stringResource(Res.string.listing_editor_label_price)) },
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
            label = { Text(stringResource(Res.string.listing_editor_label_image_url)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (imageUrl.value.isNotBlank()) {
            AsyncImage(
                model = imageUrl.value,
                contentDescription = stringResource(Res.string.listing_editor_image_preview_desc),
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
                            text = stringResource(Res.string.listing_editor_no_image_preview),
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
            Text(stringResource(Res.string.listing_editor_step_review))
        }
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectionContainer {
                Text(stringResource(Res.string.listing_editor_review_type_format, editorState.type.name))
            }
            SelectionContainer {
                Text(stringResource(Res.string.listing_editor_review_title_format, editorState.title.value))
            }
            SelectionContainer {
                Text(
                    stringResource(
                        Res.string.listing_editor_review_price_format,
                        "${editorState.priceCurrency.symbol}${editorState.priceAmount.value}"
                    )
                )
            }
            if (editorState.description.value.isNotBlank()) {
                SelectionContainer {
                    Text(
                        stringResource(
                            Res.string.listing_editor_review_description_format,
                            editorState.description.value
                        )
                    )
                }
            }
            if (editorState.imageUrl.value.isNotBlank()) {
                SelectionContainer {
                    Text(stringResource(Res.string.listing_editor_review_image_url_format, editorState.imageUrl.value))
                }
            }
        }
    }
}