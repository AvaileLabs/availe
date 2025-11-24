package com.availelabs.availe.features.listings.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.domain.ListingEditorIntent
import com.availelabs.availe.domain.ListingEditorState
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.features.listings.IListingEditorComponent
import com.availelabs.availe.ui.ScreenScaffold
import com.composables.icons.lucide.*
import com.composables.uikit.components.*
import com.composables.uikit.styling.*
import com.composeunstyled.currentWindowContainerSize
import com.composeunstyled.theme.Theme

@Composable
fun ListingEditorScreen(component: IListingEditorComponent) {
    val state by component.state.subscribeAsState()
    val focusManager = LocalFocusManager.current
    val isWide = currentWindowContainerSize().width > 600.dp

    ScreenScaffold(
        appBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = {
                        focusManager.clearFocus()
                        component.onCancel()
                    }) {
                        Icon(Lucide.X, "Cancel")
                    }
                    Text(
                        text = if (state.isEditing) "Edit Listing" else "New Listing",
                        style = Theme[textStyles][title]
                    )
                }
            }
        }
    ) {
        if (isWide) {
            DesktopLayout(
                state = state,
                component = component
            )
        } else {
            MobileLayout(
                state = state,
                component = component
            )
        }
    }
}

@Composable
private fun DesktopLayout(
    state: ListingEditorState,
    component: IListingEditorComponent
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ListingForm(state, component)
            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HeroImageSection(
                imageUrl = state.imageUrl.value,
                onUrlChange = { component.onIntent(ListingEditorIntent.ChangeImage(ImageUrl(it))) },
                modifier = Modifier.heightIn(max = 350.dp)
            )
        }
    }
}

@Composable
private fun MobileLayout(
    state: ListingEditorState,
    component: IListingEditorComponent
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        HeroImageSection(
            imageUrl = state.imageUrl.value,
            onUrlChange = { component.onIntent(ListingEditorIntent.ChangeImage(ImageUrl(it))) }
        )

        ListingForm(state, component)

        Spacer(Modifier.windowInsetsPadding(WindowInsets.safeDrawing))
    }
}

@Composable
private fun ListingForm(
    state: ListingEditorState,
    component: IListingEditorComponent
) {
    val focusManager = LocalFocusManager.current

    EssentialsSection(
        title = state.title.value,
        onTitleChange = { component.onIntent(ListingEditorIntent.ChangeTitle(ListingTitle(it))) },
        price = state.priceAmount.value,
        onPriceChange = { component.onIntent(ListingEditorIntent.ChangePrice(PriceAmount(it))) },
        currency = state.priceCurrency,
        onCurrencyChange = { component.onIntent(ListingEditorIntent.ChangeCurrency(it)) }
    )

    DetailsSection(
        type = state.type,
        onTypeChange = { component.onIntent(ListingEditorIntent.ChangeType(it)) },
        description = state.description.value,
        onDescriptionChange = { component.onIntent(ListingEditorIntent.ChangeDescription(ListingDescription(it))) },
        isEditing = state.isEditing
    )

    if (state.error != null) {
        ErrorMessage(state.error!!)
    }

    FormSubmitButton(
        isEditing = state.isEditing,
        isSubmitting = state.isSubmitting,
        onClick = {
            focusManager.clearFocus()
            component.onIntent(ListingEditorIntent.Submit)
        }
    )
}

@Composable
private fun HeroImageSection(
    imageUrl: String,
    onUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(Theme[shapes][medium])
                .background(Theme[colors][card])
                .border(1.dp, Theme[colors][outline], Theme[shapes][medium]),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Listing preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Lucide.Image,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Theme[colors][outline]
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No Image Preview", color = Color.Gray)
                }
            }
        }

        TextField(
            value = imageUrl,
            onValueChange = onUrlChange,
            placeholder = { Text("https://example.com/image.jpg") },
            modifier = Modifier.fillMaxWidth(),
            leading = { Icon(Lucide.Image, null, modifier = Modifier.size(16.dp)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
    }
}

@Composable
private fun EssentialsSection(
    title: String,
    onTitleChange: (String) -> Unit,
    price: String,
    onPriceChange: (String) -> Unit,
    currency: SupportedCurrency,
    onCurrencyChange: (SupportedCurrency) -> Unit
) {
    Card(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EditorField(label = "Title") {
                TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("What are you listing?") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Text)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    EditorField(label = "Price") {
                        TextField(
                            value = price,
                            onValueChange = onPriceChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                    }
                }

                Box(Modifier.weight(0.6f)) {
                    EditorField(label = "Currency") {
                        CurrencySelector(
                            selected = currency,
                            onSelect = onCurrencyChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsSection(
    type: ListingCategory,
    onTypeChange: (ListingCategory) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    isEditing: Boolean
) {
    Card(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            EditorField(label = "Type") {
                if (isEditing) {
                    Text(
                        text = type.name,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    SegmentedControl(
                        options = ListingCategory.entries,
                        selected = type,
                        onSelect = onTypeChange,
                        labelProvider = { it.name }
                    )
                }
            }

            EditorField(label = "Description") {
                TextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Tell us more about your listing...") },
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Text)
                )
            }
        }
    }
}

@Composable
private fun FormSubmitButton(
    isEditing: Boolean,
    isSubmitting: Boolean,
    onClick: () -> Unit
) {
    PrimaryButton(
        onClick = onClick,
        enabled = !isSubmitting,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isSubmitting) {
            IntermediateProgressIndicator(modifier = Modifier.size(16.dp))
        } else {
            Text(if (isEditing) "Save Changes" else "Publish Listing")
        }
    }
}

@Composable
private fun ErrorMessage(text: String) {
    Text(
        text = text,
        color = Theme[colors][destructive],
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun EditorField(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        content()
    }
}

@Composable
private fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelProvider: (T) -> String
) {
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme[colors][outline].copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val backgroundColor by animateColorAsState(
                if (isSelected) Theme[colors][card] else Color.Transparent
            )
            val textColor by animateColorAsState(
                if (isSelected) Theme[colors][onCard] else Color.Gray
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(backgroundColor)
                    .clickable {
                        focusManager.clearFocus()
                        onSelect(option)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelProvider(option),
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CurrencySelector(
    selected: SupportedCurrency,
    onSelect: (SupportedCurrency) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme[colors][outline].copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SupportedCurrency.entries.forEach { currency ->
            val isSelected = currency == selected
            val backgroundColor by animateColorAsState(
                if (isSelected) Theme[colors][accent] else Color.Transparent
            )
            val textColor by animateColorAsState(
                if (isSelected) Color.White else Color.Gray
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(backgroundColor)
                    .clickable {
                        focusManager.clearFocus()
                        onSelect(currency)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (currency == SupportedCurrency.USD) Lucide.DollarSign else Lucide.Euro,
                    contentDescription = currency.code,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}