package com.availelabs.availe.features.listings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.ui.ScreenScaffold
import com.availelabs.ui.utils.ContentState
import com.composables.icons.lucide.*
import com.composables.uikit.components.*
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme

@Composable
fun MyListingsScreen(component: IMyListingsHostComponent) {
    val childStack by component.childStack.subscribeAsState()

    Children(
        stack = childStack,
    ) {
        when (val child = it.instance) {
            is MyListingsChild.List -> UserListingsScreen(child.component)
            is MyListingsChild.Editor -> ListingEditorScreen(child.component)
        }
    }
}

@Composable
fun UserListingsScreen(component: IUserListingsComponent) {
    val state by component.state.subscribeAsState()
    var listingToDelete by remember { mutableStateOf<Listing?>(null) }

    if (listingToDelete != null) {
        CenteredAlert(
            visible = true,
            title = { Text("Delete Listing") },
            body = { Text("Are you sure you want to delete this listing? This action cannot be undone.") },
            positiveButton = {
                DestructiveButton(
                    onClick = {
                        listingToDelete?.let { component.onDeleteClicked(it) }
                        listingToDelete = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Delete") }
            },
            neutralButtons = {
                SecondaryButton(
                    onClick = { listingToDelete = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel") }
            }
        )
    }

    ScreenScaffold {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Listings",
                    style = Theme[textStyles][title]
                )
                IconButton(onClick = component::onRefresh) {
                    Icon(Lucide.RefreshCw, "Refresh")
                }
            }

            when (val contentState = state) {
                is ContentState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        IntermediateProgressIndicator()
                    }
                }

                is ContentState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error: ${contentState.message}")
                            Spacer(Modifier.height(16.dp))
                            PrimaryButton(onClick = { component.onRefresh() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is ContentState.Content -> {
                    val listings = contentState.data
                    if (listings.isEmpty()) {
                        EmptyState(onCreateClick = component::onCreateClicked)
                    } else {
                        ListingsList(
                            listings = listings,
                            onEditClick = component::onEditClicked,
                            onDeleteClick = { listingToDelete = it },
                            onCreateClick = component::onCreateClicked
                        )
                    }
                }

                is ContentState.Idle -> {
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onCreateClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("You haven't created any listings yet.")
            PrimaryButton(onClick = onCreateClick) {
                Text("Create New Listing")
            }
        }
    }
}

@Composable
private fun ListingsList(
    listings: List<Listing>,
    onEditClick: (Listing) -> Unit,
    onDeleteClick: (Listing) -> Unit,
    onCreateClick: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(listings) { listing ->
                UserListingItem(
                    listing = listing,
                    onEditClick = { onEditClick(listing) },
                    onDeleteClick = { onDeleteClick(listing) }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            PrimaryButton(onClick = onCreateClick) {
                Text("Create")
            }
        }
    }
}

@Composable
private fun UserListingItem(
    listing: Listing,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(listing.title)
                Text(
                    text = "${listing.price.amount} ${listing.price.currency}",
                    color = Theme[colors][accent]
                )
                Text(
                    text = when (listing) {
                        is Listing.Item -> "Item"
                        is Listing.Event -> "Event"
                        is Listing.Service -> "Service"
                    },
                    color = Color.Gray
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(Lucide.Pencil, contentDescription = "Edit")
                }

                DestructiveButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(Lucide.Trash2, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun ListingEditorScreen(component: IListingEditorComponent) {
    val state by component.state.subscribeAsState()

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
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