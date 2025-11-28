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
import com.composables.uikit.components.IntermediateProgressIndicator
import com.composables.uikit.components.PrimaryButton
import com.composables.uikit.components.Text
import kotlin.uuid.ExperimentalUuidApi

@Composable
internal fun HomeScreen(
    homeComponent: IHomeComponent,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val homeState by homeComponent.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                homeState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        IntermediateProgressIndicator()
                    }
                }

                homeState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(homeState.errorMessage!!)
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(
                            onClick = { homeComponent.onIntent(HomeStore.Intent.RefreshListings) }
                        ) {
                            Text("Retry")
                        }
                    }
                }

                homeState.listingUiModels.isEmpty() -> {
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
                        items(homeState.listingUiModels, key = { it.id.value }) { listingUiModel ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(listingUiModel.title.value)
                                Text(listingUiModel.price)
                            }
                        }
                    }
                }
            }
        }
    }
}
