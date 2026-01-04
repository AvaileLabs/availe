@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.*
import com.availelabs.availe.profile.ProfileStore.Intent
import com.availelabs.availe.ui.RefreshStateHolder
import com.availelabs.availe.ui.components.ListingCard
import com.availelabs.availe.ui.model.asString
import com.availelabs.availe.ui.modifiers.refreshTrigger
import com.availelabs.availe.ui.util.InfiniteListHandler
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.User
import com.composables.uikit.components.*
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun ProfileScreen(
    component: IProfileComponent
) {
    val state by component.state.collectAsState()
    val refreshState = remember { RefreshStateHolder() }
    val listState = rememberLazyListState()

    InfiniteListHandler(
        listState = listState,
        enabled = !state.isLoading && !state.endReached && state.paginationError == null,
        onLoadMore = { component.onIntent(Intent.LoadNextPage) }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .refreshTrigger(
                state = refreshState,
                onRefresh = { component.onIntent(Intent.RefreshRequested) }
            )
            .fillMaxSize()
            .background(Theme[colors][background])
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ProfileHeader(
                    userId = state.targetUserId.value.toString(),
                    isCurrentUser = state.isCurrentUser,
                    onMessageClick = { component.onIntent(Intent.MessageClicked) },
                    onSettingsClick = { component.onIntent(Intent.SettingsClicked) }
                )
            }

            if (state.isLoading && state.listings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        IntermediateProgressIndicator()
                    }
                }
            } else if (state.error != null && state.listings.isEmpty()) {
                item {
                    ErrorState(
                        error = state.error!!.asString(),
                        onRetry = { component.onIntent(Intent.RefreshRequested) }
                    )
                }
            } else if (state.listings.isEmpty() && !state.isLoading) {
                item {
                    EmptyState()
                }
            } else {
                items(state.listings, key = { it.id.value }) { listing ->
                    ListingCard(
                        title = listing.title,
                        price = listing.price,
                        imageUrl = listing.imageUrl,
                        category = listing.category,
                        description = listing.description,
                        onClick = { component.onIntent(Intent.ListingClicked(listing.id)) }
                    )
                }
            }

            if (state.isLoading && state.listings.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IntermediateProgressIndicator()
                    }
                }
            }
        }

        PullToRefreshIndicator(
            isRefreshing = state.isRefreshing,
            pullProgress = refreshState.pullProgress,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun ProfileHeader(
    userId: String,
    isCurrentUser: Boolean,
    onMessageClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Theme[colors][secondary]),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.User,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Theme[colors][onSecondary]
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "User Profile",
            style = Theme[textStyles][title]
        )
        Text(
            text = userId.take(8),
            style = Theme[textStyles][caption]
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isCurrentUser) {
                OutlinedButton(onClick = onSettingsClick) {
                    Icon(Lucide.Settings, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.navigation_settings))
                }
            } else {
                PrimaryButton(onClick = onMessageClick) {
                    Icon(Lucide.MessageCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.navigation_chat))
                }
            }
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = error, color = Theme[colors][destructive])
        Spacer(Modifier.height(16.dp))
        PrimaryButton(onClick = onRetry) {
            Text(stringResource(Res.string.generic_retry))
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(Res.string.home_empty_state),
            style = Theme[textStyles][body]
        )
    }
}

@Composable
private fun PullToRefreshIndicator(
    isRefreshing: Boolean,
    pullProgress: Float,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isRefreshing || pullProgress > 0,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (isRefreshing) {
            IntermediateProgressIndicator(modifier = Modifier.padding(16.dp))
        }
    }
}