package com.availelabs.availe.ui.util

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value

@Composable
fun <T : Any> rememberSyncedPagerState(
    pagesValue: Value<ChildPages<*, T>>,
    onPageSelected: (Int) -> Unit
): PagerState {
    val pages by pagesValue.subscribeAsState()
    val childPages = pages.items
    val selectedIndex = pages.selectedIndex

    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { childPages.size }
    )

    LaunchedEffect(selectedIndex) {
        if (pagerState.currentPage != selectedIndex) {
            pagerState.animateScrollToPage(selectedIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedIndex) {
            onPageSelected(pagerState.currentPage)
        }
    }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val currentPage = pagerState.currentPage
            if (currentPage != selectedIndex) {
                onPageSelected(currentPage)
            }
        }
    }

    return pagerState
}