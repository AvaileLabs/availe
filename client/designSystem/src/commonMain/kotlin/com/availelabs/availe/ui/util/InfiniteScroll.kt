package com.availelabs.availe.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*

@Composable
fun InfiniteListHandler(
    listState: LazyListState,
    buffer: Int = 5,
    enabled: Boolean = true,
    onLoadMore: () -> Unit
) {
    val shouldLoadMore by remember(enabled) {
        derivedStateOf {
            if (!enabled) return@derivedStateOf false
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            lastVisibleItemIndex > (totalItemsNumber - buffer)
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }
}

@Composable
fun InfiniteGridHandler(
    gridState: LazyGridState,
    buffer: Int = 10,
    enabled: Boolean = true,
    onLoadMore: () -> Unit
) {
    val shouldLoadMore by remember(enabled) {
        derivedStateOf {
            if (!enabled) return@derivedStateOf false
            val layoutInfo = gridState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            lastVisibleItemIndex > (totalItemsNumber - buffer)
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }
}