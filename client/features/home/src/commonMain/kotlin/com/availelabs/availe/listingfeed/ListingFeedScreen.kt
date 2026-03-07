package com.availelabs.availe.listingfeed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.*
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.navigation.DeepLinkRoute
import com.availelabs.availe.ui.RefreshStateHolder
import com.availelabs.availe.ui.components.LinkContextMenuArea
import com.availelabs.availe.ui.components.ListingCard
import com.availelabs.availe.ui.layout.WindowSizeClass
import com.availelabs.availe.ui.layout.rememberWindowSize
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
import com.composables.uikit.styling.*
import com.composeunstyled.outline
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
                    isRefreshing = state.isRefreshing,
                    pullProgress = refreshState.pullProgress,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                if (state.isSyncing) {
                    AnimatedLinearProgressIndicator(
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedLinearProgressIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val translation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clipToBounds()
    ) {
        val barWidth = maxWidth * 0.4f
        val maxOffset = maxWidth + barWidth
        val currentOffset = (maxOffset * translation) - barWidth

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(barWidth)
                .offset(x = currentOffset)
                .background(Theme[colors][primary])
        )
    }
}

@Composable
private fun WithTouchSlop(
    touchSlop: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val base = LocalViewConfiguration.current
    val density = LocalDensity.current
    val slopPx = remember(touchSlop, density) { with(density) { touchSlop.toPx() } }

    val patched = remember(base, slopPx) {
        object : ViewConfiguration by base {
            override val touchSlop: Float = slopPx
        }
    }

    CompositionLocalProvider(LocalViewConfiguration provides patched) {
        content()
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
        enabled = !state.isLoading && !state.endReached && state.paginationError == null,
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
        if (state.isLoading && state.listingUiModels.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IntermediateProgressIndicator()
                }
            }
        } else if (state.inlineError != null && state.listingUiModels.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorState(error = state.inlineError.asString(), onRetry = onRefresh)
            }
        } else if (state.listingUiModels.isEmpty() && !state.isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(onRefresh = onRefresh)
            }
        } else {
            items(state.listingUiModels, key = { it.id.value }) { listingUiModel ->
                ListingItem(
                    listingUiModel = listingUiModel,
                    onListingClick = onListingClick
                )
            }

            if (state.isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IntermediateProgressIndicator()
                    }
                }
            }

            if (state.paginationError != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PaginationErrorFooter(
                        error = state.paginationError.asString(),
                        onRetry = onLoadNextPage
                    )
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
        enabled = !state.isLoading && !state.endReached && state.paginationError == null,
        onLoadMore = onLoadNextPage
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (state.isLoading && state.listingUiModels.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    IntermediateProgressIndicator()
                }
            }
        } else if (state.inlineError != null && state.listingUiModels.isEmpty()) {
            item {
                ErrorState(
                    error = state.inlineError.asString(),
                    onRetry = onRefresh,
                    modifier = Modifier.fillParentMaxSize()
                )
            }
        } else if (state.listingUiModels.isEmpty() && !state.isLoading) {
            item {
                EmptyState(
                    onRefresh = onRefresh,
                    modifier = Modifier.fillParentMaxSize()
                )
            }
        } else {
            items(state.listingUiModels, key = { it.id.value }) { listingUiModel ->
                ListingItem(
                    listingUiModel = listingUiModel,
                    onListingClick = onListingClick
                )
            }
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IntermediateProgressIndicator()
                    }
                }
            }
            if (state.paginationError != null) {
                item {
                    PaginationErrorFooter(
                        error = state.paginationError.asString(),
                        onRetry = onLoadNextPage
                    )
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