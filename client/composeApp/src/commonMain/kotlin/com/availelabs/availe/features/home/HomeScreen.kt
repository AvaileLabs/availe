@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import availe.client.composeapp.generated.resources.*
import com.availelabs.availe.ui.RefreshStateHolder
import com.availelabs.availe.ui.components.ListingCard
import com.availelabs.availe.ui.components.layout.WindowSizeClass
import com.availelabs.availe.ui.components.layout.rememberWindowSize
import com.availelabs.availe.ui.model.asString
import com.availelabs.availe.ui.modifiers.refreshTrigger
import com.availelabs.availe.ui.util.InfiniteGridHandler
import com.availelabs.availe.ui.util.InfiniteListHandler
import com.composables.icons.lucide.ArrowDown
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.Icon
import com.composables.uikit.components.IntermediateProgressIndicator
import com.composables.uikit.components.PrimaryButton
import com.composables.uikit.components.Text
import com.composables.uikit.styling.card
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.outline
import com.composeunstyled.outline
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
internal fun HomeScreen(
    homeComponent: IHomeComponent,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val homeState by homeComponent.state.collectAsState()
    val refreshState = remember { RefreshStateHolder() }
    val windowSize = rememberWindowSize()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .refreshTrigger(
                    state = refreshState,
                    onRefresh = { homeComponent.onIntent(HomeStore.Intent.RefreshListingsRequested) }
                )
        ) {
            when (windowSize) {
                WindowSizeClass.Compact -> {
                    MobileListLayout(
                        homeState = homeState,
                        onListingClick = { homeComponent.onIntent(HomeStore.Intent.ListingClicked(it)) },
                        onRefresh = { homeComponent.onIntent(HomeStore.Intent.RefreshListingsRequested) },
                        onLoadNextPage = { homeComponent.onIntent(HomeStore.Intent.LoadNextPage) }
                    )
                }

                WindowSizeClass.Medium,
                WindowSizeClass.Expanded -> {
                    DesktopGridLayout(
                        homeState = homeState,
                        onListingClick = { homeComponent.onIntent(HomeStore.Intent.ListingClicked(it)) },
                        onRefresh = { homeComponent.onIntent(HomeStore.Intent.RefreshListingsRequested) },
                        onLoadNextPage = { homeComponent.onIntent(HomeStore.Intent.LoadNextPage) },
                        isExpanded = windowSize is WindowSizeClass.Expanded
                    )
                }
            }

            PullToRefreshIndicator(
                isRefreshing = homeState.isRefreshing,
                pullProgress = refreshState.pullProgress,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun DesktopGridLayout(
    homeState: HomeStore.State,
    onListingClick: (com.availelabs.availe.domain.model.ListingId) -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    isExpanded: Boolean
) {
    val gridState = rememberLazyGridState()

    InfiniteGridHandler(
        gridState = gridState,
        enabled = !homeState.isLoading && !homeState.endReached && homeState.paginationError == null,
        onLoadMore = onLoadNextPage
    )

    val minColumnSize = if (isExpanded) 300.dp else 240.dp

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minColumnSize),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(8.dp))
        }

        if (homeState.isLoading && homeState.listingUiModels.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IntermediateProgressIndicator()
                }
            }
        } else if (homeState.inlineError != null && homeState.listingUiModels.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorState(error = homeState.inlineError.asString(), onRetry = onRefresh)
            }
        } else if (homeState.listingUiModels.isEmpty() && !homeState.isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(onRefresh = onRefresh)
            }
        } else {
            items(homeState.listingUiModels, key = { it.id.value }) { listingUiModel ->
                ListingCard(
                    title = listingUiModel.title,
                    price = listingUiModel.price,
                    imageUrl = listingUiModel.imageUrl,
                    category = listingUiModel.category,
                    description = listingUiModel.description,
                    onClick = { onListingClick(listingUiModel.id) }
                )
            }

            if (homeState.isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IntermediateProgressIndicator()
                    }
                }
            }

            if (homeState.paginationError != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PaginationErrorFooter(
                        error = homeState.paginationError.asString(),
                        onRetry = onLoadNextPage
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MobileListLayout(
    homeState: HomeStore.State,
    onListingClick: (com.availelabs.availe.domain.model.ListingId) -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit
) {
    val listState = rememberLazyListState()

    InfiniteListHandler(
        listState = listState,
        enabled = !homeState.isLoading && !homeState.endReached && homeState.paginationError == null,
        onLoadMore = onLoadNextPage
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState
    ) {
        if (homeState.isLoading && homeState.listingUiModels.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    IntermediateProgressIndicator()
                }
            }
        } else if (homeState.inlineError != null && homeState.listingUiModels.isEmpty()) {
            item {
                ErrorState(
                    error = homeState.inlineError.asString(),
                    onRetry = onRefresh,
                    modifier = Modifier.fillParentMaxSize()
                )
            }
        } else if (homeState.listingUiModels.isEmpty() && !homeState.isLoading) {
            item {
                EmptyState(
                    onRefresh = onRefresh,
                    modifier = Modifier.fillParentMaxSize()
                )
            }
        } else {
            items(homeState.listingUiModels, key = { it.id.value }) { listingUiModel ->
                ListingCard(
                    title = listingUiModel.title,
                    price = listingUiModel.price,
                    imageUrl = listingUiModel.imageUrl,
                    category = listingUiModel.category,
                    description = listingUiModel.description,
                    onClick = { onListingClick(listingUiModel.id) }
                )
            }
            if (homeState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IntermediateProgressIndicator()
                    }
                }
            }
            if (homeState.paginationError != null) {
                item {
                    PaginationErrorFooter(
                        error = homeState.paginationError.asString(),
                        onRetry = onLoadNextPage
                    )
                }
            }
        }
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
            Text(error)
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

@Composable
private fun PullToRefreshIndicator(
    isRefreshing: Boolean,
    pullProgress: Float,
    modifier: Modifier = Modifier
) {
    val showIndicator = isRefreshing || pullProgress > 0f

    AnimatedVisibility(
        visible = showIndicator,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val yOffset = if (isRefreshing) 16.dp else (pullProgress * 48).dp

        Box(
            modifier = Modifier
                .offset { IntOffset(0, yOffset.roundToPx()) }
                .shadow(4.dp, CircleShape)
                .background(Theme[colors][card], CircleShape)
                .outline(1.dp, Theme[colors][outline], CircleShape)
                .padding(8.dp)
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRefreshing) {
                IntermediateProgressIndicator(
                    modifier = Modifier.size(20.dp)
                )
            } else {
                val rotation by animateFloatAsState(targetValue = pullProgress * 180f)
                val scale by animateFloatAsState(targetValue = pullProgress.coerceIn(0f, 1f))

                Box(
                    modifier = Modifier
                        .rotate(rotation)
                        .scale(scale)
                ) {
                    Icon(
                        imageVector = Lucide.ArrowDown,
                        contentDescription = stringResource(Res.string.accessibility_pull_to_refresh),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}