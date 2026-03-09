package com.availelabs.availe.ui.model

sealed interface SyncTrigger {
    data object InitialLoad : SyncTrigger
    data object PullToRefresh : SyncTrigger
    data object Pagination : SyncTrigger
    data object BackgroundUpdate : SyncTrigger
}

sealed interface NetworkSyncState {
    data object Idle : NetworkSyncState
    data class Active(val trigger: SyncTrigger) : NetworkSyncState
    data class Error(val error: UiText, val trigger: SyncTrigger) : NetworkSyncState
}

sealed interface DatabaseState<out T> {
    data object Uninitialized : DatabaseState<Nothing>
    data object Empty : DatabaseState<Nothing>
    data class Populated<out T>(val items: List<T>) : DatabaseState<T>
}

sealed interface PaginationState {
    data object Available : PaginationState
    data object EndReached : PaginationState
}

interface IFeedUiState<T> {
    val databaseState: DatabaseState<T>
    val networkSyncState: NetworkSyncState
    val paginationState: PaginationState

    val isRefreshing: Boolean
        get() {
            val currentNetworkSyncState = networkSyncState
            return currentNetworkSyncState is NetworkSyncState.Active && currentNetworkSyncState.trigger == SyncTrigger.PullToRefresh
        }

    val showCenterSpinner: Boolean
        get() {
            val currentDatabaseState = databaseState
            val currentNetworkSyncState = networkSyncState
            return currentDatabaseState is DatabaseState.Empty && currentNetworkSyncState is NetworkSyncState.Active && currentNetworkSyncState.trigger == SyncTrigger.InitialLoad
        }

    val showEmptyState: Boolean
        get() {
            val currentDatabaseState = databaseState
            val currentNetworkSyncState = networkSyncState
            return currentDatabaseState is DatabaseState.Empty && currentNetworkSyncState is NetworkSyncState.Idle
        }

    val showInlineError: Boolean
        get() {
            val currentDatabaseState = databaseState
            val currentNetworkSyncState = networkSyncState
            return currentDatabaseState is DatabaseState.Empty && currentNetworkSyncState is NetworkSyncState.Error
        }

    val inlineErrorMessage: UiText?
        get() {
            val currentDatabaseState = databaseState
            val currentNetworkSyncState = networkSyncState
            return if (currentDatabaseState is DatabaseState.Empty && currentNetworkSyncState is NetworkSyncState.Error) {
                currentNetworkSyncState.error
            } else {
                null
            }
        }

    val showListings: Boolean
        get() {
            val currentDatabaseState = databaseState
            return currentDatabaseState is DatabaseState.Populated
        }

    val isTopSyncing: Boolean
        get() {
            val currentNetworkSyncState = networkSyncState
            return showListings && currentNetworkSyncState is NetworkSyncState.Active && (currentNetworkSyncState.trigger == SyncTrigger.InitialLoad || currentNetworkSyncState.trigger == SyncTrigger.BackgroundUpdate)
        }

    val showPaginationSpinner: Boolean
        get() {
            val currentNetworkSyncState = networkSyncState
            return showListings && currentNetworkSyncState is NetworkSyncState.Active && currentNetworkSyncState.trigger == SyncTrigger.Pagination
        }

    val canLoadMore: Boolean
        get() {
            val currentPaginationState = paginationState
            val currentNetworkSyncState = networkSyncState
            return currentPaginationState is PaginationState.Available && currentNetworkSyncState is NetworkSyncState.Idle
        }

    val showPaginationError: Boolean
        get() {
            val currentNetworkSyncState = networkSyncState
            return showListings && currentNetworkSyncState is NetworkSyncState.Error && currentNetworkSyncState.trigger == SyncTrigger.Pagination
        }

    val listings: List<T>
        get() {
            val currentDatabaseState = databaseState
            return if (currentDatabaseState is DatabaseState.Populated) {
                currentDatabaseState.items
            } else {
                emptyList()
            }
        }
}