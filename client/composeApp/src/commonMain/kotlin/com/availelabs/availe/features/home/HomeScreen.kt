@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.availelabs.availe.ui.RefreshStateHolder
import com.availelabs.availe.ui.modifiers.refreshTrigger
import com.composables.icons.lucide.ArrowDown
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.Icon
import com.composables.uikit.components.IntermediateProgressIndicator
import com.composables.uikit.components.PrimaryButton
import com.composables.uikit.components.Text
import com.composables.uikit.styling.card
import com.composables.uikit.styling.colors
import com.composeunstyled.theme.Theme
import kotlin.uuid.ExperimentalUuidApi

@Composable
internal fun HomeScreen(
    homeComponent: IHomeComponent,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val homeState by homeComponent.state.collectAsState()
    val refreshState = remember { RefreshStateHolder() }

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
                            onClick = { homeComponent.onIntent(HomeStore.Intent.RefreshListingsRequested) }
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
                                onClick = { homeComponent.onIntent(HomeStore.Intent.RefreshListingsRequested) }
                            ) {
                                Text("Refresh")
                            }
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .refreshTrigger(
                                state = refreshState,
                                onRefresh = { homeComponent.onIntent(HomeStore.Intent.RefreshListingsRequested) }
                            )
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(homeState.listingUiModels, key = { it.id.value }) { listingUiModel ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            homeComponent.onIntent(HomeStore.Intent.ListingClicked(listingUiModel.id))
                                        }
                                        .padding(16.dp)
                                ) {
                                    Text(listingUiModel.title.value)
                                    Text(listingUiModel.price)
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