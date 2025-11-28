@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.LogIn
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.*
import kotlin.uuid.ExperimentalUuidApi

@Composable
internal fun HomeScreen(
    homeComponent: IHomeComponent,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val state by homeComponent.state.collectAsState()

    AppScaffold {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.isAuthenticated) {
                        IconButton(
                            onClick = { homeComponent.onIntent(HomeStore.Intent.QuickCreateListing) }
                        ) {
                            Icon(Lucide.CirclePlus, "Add")
                        }
                        IconButton(
                            onClick = { homeComponent.onIntent(HomeStore.Intent.LogoutClicked) }
                        ) {
                            Icon(Lucide.LogOut, "Logout")
                        }
                    } else {
                        PrimaryButton(
                            onClick = { homeComponent.onIntent(HomeStore.Intent.LoginClicked) },
                            enabled = state.isAuthServiceAvailable
                        ) {
                            Icon(Lucide.LogIn, null)
                            Text("Log In")
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            IntermediateProgressIndicator()
                        }
                    }

                    state.errorMessage != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(state.errorMessage!!)
                            Spacer(Modifier.height(16.dp))
                            PrimaryButton(
                                onClick = { homeComponent.onIntent(HomeStore.Intent.RefreshListings) }
                            ) {
                                Text("Retry")
                            }
                        }
                    }

                    state.listingUiModels.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No listings found")
                                Spacer(Modifier.height(16.dp))
                                PrimaryButton(
                                    onClick = { homeComponent.onIntent(HomeStore.Intent.RefreshListings) }
                                ) {
                                    Text("Refresh")
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.listingUiModels, key = { it.id.value }) { listing ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(listing.title.value)
                                    Text(listing.price)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}