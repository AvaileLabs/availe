package com.availelabs.availe.ui.util

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.collectLatest

private enum class PagerScrollInteractionSource {
    UserDragOrFling,
    ProgrammaticAnimation
}

@Composable
fun <T : Any> rememberSyncedPagerState(
    pagesValue: Value<ChildPages<*, T>>,
    onPageSelected: (Int) -> Unit
): PagerState {
    val pages by pagesValue.subscribeAsState()
    val childPages = pages.items
    val selectedIndex = pages.selectedIndex

    val currentOnPageSelected by rememberUpdatedState(onPageSelected)

    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { childPages.size }
    )

    var scrollInteractionSource by remember { mutableStateOf(PagerScrollInteractionSource.UserDragOrFling) }

    LaunchedEffect(selectedIndex) {
        if (pagerState.currentPage != selectedIndex) {
            scrollInteractionSource = PagerScrollInteractionSource.ProgrammaticAnimation
            try {
                pagerState.animateScrollToPage(selectedIndex)
            } finally {
                scrollInteractionSource = PagerScrollInteractionSource.UserDragOrFling
            }
        }
    }

    LaunchedEffect(pagerState, selectedIndex) {
        snapshotFlow { pagerState.currentPage }
            .collectLatest { page ->
                val isUserInteraction = scrollInteractionSource == PagerScrollInteractionSource.UserDragOrFling
                if (isUserInteraction && page != selectedIndex) {
                    currentOnPageSelected(page)
                }
            }
    }

    return pagerState
}