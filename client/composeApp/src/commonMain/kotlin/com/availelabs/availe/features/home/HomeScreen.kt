package com.availelabs.availe.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.availelabs.availe.ui.RefreshStateHolder
import com.availelabs.availe.ui.components.ListingCard
import com.availelabs.availe.ui.modifiers.refreshTrigger
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
    val listState = rememberLazyListState()

    val shouldLoadNextPage by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > (totalItemsNumber - 5)
        }
    }

    LaunchedEffect(shouldLoadNextPage) {
        if (shouldLoadNextPage && !homeState.isLoading && !homeState.endReached) {
            homeComponent.onIntent(HomeStore.Intent.LoadNextPage)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .refreshTrigger(
                    state = refreshState,
                    onRefresh = { homeComponent.onIntent(HomeStore.Intent.RefreshListingsRequested) }
                )
        ) {
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
                } else if (homeState.errorMessage != null && homeState.listingUiModels.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            SelectionContainer {
                                Text(homeState.errorMessage!!)
                            }
                            Spacer(Modifier.height(16.dp))
                            PrimaryButton(
                                onClick = { homeComponent.onIntent(HomeStore.Intent.RefreshListingsRequested) }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                } else if (homeState.listingUiModels.isEmpty() && !homeState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                SelectionContainer {
                                    Text("No listings found")
                                }
                                Spacer(Modifier.height(16.dp))
                                PrimaryButton(
                                    onClick = { homeComponent.onIntent(HomeStore.Intent.RefreshListingsRequested) }
                                ) {
                                    Text("Refresh")
                                }
                            }
                        }
                    }
                } else {
                    items(homeState.listingUiModels, key = { it.id.value }) { listingUiModel ->
                        ListingCard(
                            title = listingUiModel.title,
                            price = listingUiModel.price,
                            imageUrl = listingUiModel.imageUrl,
                            category = listingUiModel.category,
                            description = listingUiModel.description,
                            onClick = {
                                homeComponent.onIntent(HomeStore.Intent.ListingClicked(listingUiModel.id))
                            }
                        )
                    }
                    if (homeState.isLoading && homeState.listingUiModels.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                IntermediateProgressIndicator()
                            }
                        }
                    }
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
                        contentDescription = "Pull to refresh",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}