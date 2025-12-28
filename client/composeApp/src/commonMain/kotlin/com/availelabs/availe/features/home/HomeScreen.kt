package com.availelabs.availe.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.features.listingfeed.ListingFeedScreen
import com.availelabs.availe.ui.util.rememberSyncedPagerState
import com.composables.uikit.components.Text
import com.composables.uikit.styling.accent
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.onBackground
import com.composables.uikit.styling.primary
import com.composeunstyled.theme.Theme

@Composable
internal fun HomeScreen(
    homeComponent: IHomeComponent,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val pages by homeComponent.pages.subscribeAsState()
    val childPages = pages.items
    val selectedIndex = pages.selectedIndex

    val pagerState = rememberSyncedPagerState(
        pagesValue = homeComponent.pages,
        onPageSelected = homeComponent::onTabClicked
    )

    Column(modifier = Modifier.fillMaxSize()) {
        HomeTabRow(
            selectedIndex = selectedIndex,
            onTabSelected = { index ->
                homeComponent.onTabClicked(index)
            }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val child = childPages[pageIndex]
            val instance = child.instance

            if (instance != null) {
                ListingFeedScreen(component = instance)
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun HomeTabRow(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(ListingCategory.Item, ListingCategory.Event, ListingCategory.Service)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, category ->
            val isSelected = index == selectedIndex
            val color = if (isSelected) Theme[colors][primary] else Theme[colors][onBackground].copy(alpha = 0.6f)
            val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = category.name,
                        color = color,
                        fontWeight = fontWeight,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .fillMaxWidth(0.6f)
                                .background(Theme[colors][accent])
                        )
                    }
                }
            }
        }
    }
}