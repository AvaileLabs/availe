@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.profile

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.*
import coil3.compose.AsyncImage
import com.availelabs.availe.domain.model.ImageUrl
import com.availelabs.availe.navigation.DeepLinkRoute
import com.availelabs.availe.profile.ProfileStore.Intent
import com.availelabs.availe.ui.RefreshStateHolder
import com.availelabs.availe.ui.components.*
import com.availelabs.availe.ui.model.*
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

    val currentNetworkSyncState = state.networkSyncState
    val currentDatabaseState = state.databaseState

    val isRefreshing =
        currentNetworkSyncState is NetworkSyncState.Active && currentNetworkSyncState.trigger == SyncTrigger.PullToRefresh
    val isSyncing =
        currentDatabaseState is DatabaseState.Populated && currentNetworkSyncState is NetworkSyncState.Active && (currentNetworkSyncState.trigger == SyncTrigger.InitialLoad || currentNetworkSyncState.trigger == SyncTrigger.BackgroundUpdate)

    InfiniteListHandler(
        listState = listState,
        enabled = state.paginationState is PaginationState.Available && state.networkSyncState is NetworkSyncState.Idle,
        onLoadMore = { component.onIntent(Intent.LoadNextPage) }
    )

    WithTouchSlop(touchSlop = 8.dp) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .refreshTrigger(
                    state = refreshState,
                    onRefresh = { component.onIntent(Intent.RefreshRequested) }
                )
                .background(Theme[colors][background])
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    ProfileHeader(
                        userId = state.targetUserId.value.toString(),
                        displayName = state.profile?.displayName?.value,
                        avatarUrl = state.profile?.avatarUrl,
                        isCurrentUser = state.isCurrentUser,
                        onMessageClick = { component.onIntent(Intent.MessageClicked) },
                        onSettingsClick = { component.onIntent(Intent.SettingsClicked) }
                    )
                }

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
                                        EmptyState()
                                    }
                                }
                            }

                            is NetworkSyncState.Error -> {
                                item {
                                    ErrorState(
                                        error = networkSyncState.error.asString(),
                                        onRetry = { component.onIntent(Intent.RefreshRequested) }
                                    )
                                }
                            }

                            is NetworkSyncState.Idle -> {
                                item {
                                    EmptyState()
                                }
                            }
                        }
                    }

                    is DatabaseState.Populated -> {
                        items(databaseState.items, key = { it.id.value }) { listing ->
                            LinkContextMenuArea(route = DeepLinkRoute.ListingDetails(listing.id)) {
                                ListingCard(
                                    title = listing.title,
                                    price = listing.price,
                                    imageUrl = listing.imageUrl,
                                    category = listing.category,
                                    description = listing.description,
                                    startTime = listing.startTime,
                                    isRsvped = listing.isRsvped,
                                    onClick = { component.onIntent(Intent.ListingClicked(listing.id)) }
                                )
                            }
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
                                    onRetry = { component.onIntent(Intent.LoadNextPage) }
                                )
                            }
                        }
                    }
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

@Composable
private fun ProfileHeader(
    userId: String,
    displayName: String?,
    avatarUrl: ImageUrl?,
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
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl.value,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Lucide.User,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Theme[colors][onSecondary]
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = displayName ?: stringResource(Res.string.profile_title),
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
        Text(error, color = Theme[colors][destructive])
        Spacer(Modifier.height(8.dp))
        PrimaryButton(onClick = onRetry) {
            Text(stringResource(Res.string.generic_retry))
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