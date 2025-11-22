package com.availelabs.availe.features.listings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.ui.ScreenScaffold
import com.availelabs.ui.utils.ContentState
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.composables.uikit.components.*
import com.composables.uikit.styling.accent
import com.composables.uikit.styling.colors
import com.composeunstyled.theme.Theme

@Composable
fun MyListingsScreen(component: IMyListingsHostComponent) {
    val childStack by component.childStack.subscribeAsState()

    Children(
        stack = childStack,
    ) {
        when (val child = it.instance) {
            is MyListingsChild.List -> UserListingsScreen(child.component)
            is MyListingsChild.Create -> CreateListingScreen(child.component)
        }
    }
}

@Composable
fun UserListingsScreen(component: IUserListingsComponent) {
    val state by component.state.subscribeAsState()

    ScreenScaffold {
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
                        onDeleteClick = component::onDeleteClicked
                    )
                }
            }

            is ContentState.Idle -> {
                // No-op
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
    onDeleteClick: (Listing) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(listings) { listing ->
            UserListingItem(listing = listing, onDeleteClick = { onDeleteClick(listing) })
        }
    }
}

@Composable
private fun UserListingItem(
    listing: Listing,
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

@Composable
fun CreateListingScreen(component: ICreateListingComponent) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PrimaryButton(onClick = { component.onFinished() }) {
            Text("Finish Creation (Placeholder)")
        }
    }
}