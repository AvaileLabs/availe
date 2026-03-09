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