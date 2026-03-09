package com.availelabs.availe.listingfeed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.generic_refresh
import availe.client.designsystem.generated.resources.generic_retry
import availe.client.designsystem.generated.resources.home_empty_state
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.navigation.DeepLinkRoute
import com.availelabs.availe.ui.RefreshStateHolder
import com.availelabs.availe.ui.components.*
import com.availelabs.availe.ui.layout.WindowSizeClass
import com.availelabs.availe.ui.layout.rememberWindowSize
import com.availelabs.availe.ui.model.*
import com.availelabs.availe.ui.modifiers.refreshTrigger
import com.availelabs.availe.ui.util.InfiniteGridHandler
import com.availelabs.availe.ui.util.InfiniteListHandler
import com.composables.uikit.components.IntermediateProgressIndicator
import com.composables.uikit.components.PrimaryButton
import com.composables.uikit.components.Text
import com.composables.uikit.styling.background
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.destructive
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun ListingFeedScreen(
    component: IListingFeedComponent
) {
    val state by component.state.subscribeAsState()
    val refreshState = remember { RefreshStateHolder() }
    val windowSize = rememberWindowSize()

    val currentNetworkSyncState = state.networkSyncState
    val currentDatabaseState = state.databaseState

    val isRefreshing =
        currentNetworkSyncState is NetworkSyncState.Active && currentNetworkSyncState.trigger == SyncTrigger.PullToRefresh
    val isSyncing =
        currentDatabaseState is DatabaseState.Populated && currentNetworkSyncState is NetworkSyncState.Active && (currentNetworkSyncState.trigger == SyncTrigger.InitialLoad || currentNetworkSyncState.trigger == SyncTrigger.BackgroundUpdate)

    WithTouchSlop(touchSlop = 8.dp) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .refreshTrigger(
                        state = refreshState,
                        onRefresh = { component.onIntent(ListingFeedStore.Intent.RefreshListingsRequested) }
                    )
                    .background(Theme[colors][background])
            ) {
                when (windowSize) {
                    WindowSizeClass.Compact -> {
                        MobileListLayout(
                            state = state,
                            onListingClick = {
                                component.onIntent(
                                    ListingFeedStore.Intent.ListingClicked(
                                        it
                                    )
                                )
                            },
                            onRefresh = { component.onIntent(ListingFeedStore.Intent.RefreshListingsRequested) },
                            onLoadNextPage = { component.onIntent(ListingFeedStore.Intent.LoadNextPage) }
                        )
                    }

                    WindowSizeClass.Medium,
                    WindowSizeClass.Expanded -> {
                        DesktopGridLayout(
                            state = state,
                            onListingClick = {
                                component.onIntent(
                                    ListingFeedStore.Intent.ListingClicked(
                                        it
                                    )
                                )
                            },
                            onRefresh = { component.onIntent(ListingFeedStore.Intent.RefreshListingsRequested) },
                            onLoadNextPage = { component.onIntent(ListingFeedStore.Intent.LoadNextPage) },
                            isExpanded = windowSize is WindowSizeClass.Expanded
                        )
                    }
                }

                PullToRefreshIndicator(
                    isRefreshing = isRefreshing,
                    pullProgress = refreshState.pullProgress,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                if (isSyncing) {
                    LinearSyncIndicator(
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun DesktopGridLayout(
    state: ListingFeedStore.State,
    onListingClick: (ListingId) -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    isExpanded: Boolean
) {
    val gridState = rememberLazyGridState()

    InfiniteGridHandler(
        gridState = gridState,
        enabled = state.paginationState is PaginationState.Available && state.networkSyncState is NetworkSyncState.Idle,
        onLoadMore = onLoadNextPage
    )

    val minColumnSize = if (isExpanded) 300.dp else 240.dp

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minColumnSize),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        when (val databaseState = state.databaseState) {
            is DatabaseState.Uninitialized -> {
            }

            is DatabaseState.Empty -> {
                when (val networkSyncState = state.networkSyncState) {
                    is NetworkSyncState.Active -> {
                        if (networkSyncState.trigger == SyncTrigger.InitialLoad) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(400.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IntermediateProgressIndicator()
                                }
                            }
                        } else {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyState(onRefresh = onRefresh)
                            }
                        }
                    }

                    is NetworkSyncState.Error -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ErrorState(error = networkSyncState.error.asString(), onRetry = onRefresh)
                        }
                    }

                    is NetworkSyncState.Idle -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState(onRefresh = onRefresh)
                        }
                    }
                }
            }

            is DatabaseState.Populated -> {
                items(databaseState.items, key = { it.id.value }) { listingUiModel ->
                    ListingItem(
                        listingUiModel = listingUiModel,
                        onListingClick = onListingClick
                    )
                }

                val networkSyncState = state.networkSyncState
                if (networkSyncState is NetworkSyncState.Active && networkSyncState.trigger == SyncTrigger.Pagination) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IntermediateProgressIndicator()
                        }
                    }
                } else if (networkSyncState is NetworkSyncState.Error && networkSyncState.trigger == SyncTrigger.Pagination) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        PaginationErrorFooter(
                            error = networkSyncState.error.asString(),
                            onRetry = onLoadNextPage
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun MobileListLayout(
    state: ListingFeedStore.State,
    onListingClick: (ListingId) -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit
) {
    val listState = rememberLazyListState()

    InfiniteListHandler(
        listState = listState,
        enabled = state.paginationState is PaginationState.Available && state.networkSyncState is NetworkSyncState.Idle,
        onLoadMore = onLoadNextPage
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        when (val databaseState = state.databaseState) {
            is DatabaseState.Uninitialized -> {
            }

            is DatabaseState.Empty -> {
                when (val networkSyncState = state.networkSyncState) {
                    is NetworkSyncState.Active -> {
                        if (networkSyncState.trigger == SyncTrigger.InitialLoad) {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IntermediateProgressIndicator()
                                }
                            }
                        } else {
                            item {
                                EmptyState(
                                    onRefresh = onRefresh,
                                    modifier = Modifier.fillParentMaxSize()
                                )
                            }
                        }
                    }

                    is NetworkSyncState.Error -> {
                        item {
                            ErrorState(
                                error = networkSyncState.error.asString(),
                                onRetry = onRefresh,
                                modifier = Modifier.fillParentMaxSize()
                            )
                        }
                    }

                    is NetworkSyncState.Idle -> {
                        item {
                            EmptyState(
                                onRefresh = onRefresh,
                                modifier = Modifier.fillParentMaxSize()
                            )
                        }
                    }
                }
            }

            is DatabaseState.Populated -> {
                items(databaseState.items, key = { it.id.value }) { listingUiModel ->
                    ListingItem(
                        listingUiModel = listingUiModel,
                        onListingClick = onListingClick
                    )
                }

                val networkSyncState = state.networkSyncState
                if (networkSyncState is NetworkSyncState.Active && networkSyncState.trigger == SyncTrigger.Pagination) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IntermediateProgressIndicator()
                        }
                    }
                } else if (networkSyncState is NetworkSyncState.Error && networkSyncState.trigger == SyncTrigger.Pagination) {
                    item {
                        PaginationErrorFooter(
                            error = networkSyncState.error.asString(),
                            onRetry = onLoadNextPage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListingItem(
    listingUiModel: ListingUiModel,
    onListingClick: (ListingId) -> Unit
) {
    LinkContextMenuArea(route = DeepLinkRoute.ListingDetails(listingUiModel.id)) {
        ListingCard(
            title = listingUiModel.title,
            price = listingUiModel.price,
            imageUrl = listingUiModel.imageUrl,
            category = listingUiModel.category,
            description = listingUiModel.description,
            startTime = listingUiModel.startTime,
            isRsvped = listingUiModel.isRsvped,
            onClick = { onListingClick(listingUiModel.id) }
        )
    }
}

@Composable
private fun PaginationErrorFooter(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(error)
        Spacer(Modifier.height(8.dp))
        PrimaryButton(onClick = onRetry) {
            Text(stringResource(Res.string.generic_retry))
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SelectionContainer {
            Text(error, color = Theme[colors][destructive])
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(onClick = onRetry) {
            Text(stringResource(Res.string.generic_retry))
        }
    }
}

@Composable
private fun EmptyState(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SelectionContainer {
            Text(stringResource(Res.string.home_empty_state))
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(onClick = onRefresh) {
            Text(stringResource(Res.string.generic_refresh))
        }
    }
}