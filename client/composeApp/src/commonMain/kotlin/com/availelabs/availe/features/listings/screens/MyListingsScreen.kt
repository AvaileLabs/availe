package com.availelabs.availe.features.listings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.features.listings.IMyListingsHostComponent
import com.availelabs.availe.features.listings.IUserListingsComponent
import com.availelabs.availe.features.listings.MyListingsChild
import com.availelabs.availe.ui.ScreenScaffold
import com.availelabs.ui.utils.ContentState
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Trash2
import com.composables.uikit.components.*
import com.composables.uikit.styling.accent
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.textStyles
import com.composables.uikit.styling.title
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