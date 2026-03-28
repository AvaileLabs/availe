@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.navigation.DeepLinkRoute
import com.availelabs.availe.ui.model.AgendaUiModel
import com.composables.uikit.components.Text
import com.composables.uikit.styling.textStyles
import com.composables.uikit.styling.title
import com.composeunstyled.theme.Theme
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun AgendaCarousel(
    items: List<AgendaUiModel>,
    carouselTitle: String,
    onListingClick: (ListingId) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = carouselTitle,
            style = Theme[textStyles][title],
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            flingBehavior = rememberSnapFlingBehavior(lazyListState),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items, key = { it.id.value }) { agendaUiModel ->
                LinkContextMenuArea(route = DeepLinkRoute.ListingDetails(agendaUiModel.id)) {
                    AgendaCard(
                        agendaUiModel = agendaUiModel,
                        onClick = { onListingClick(agendaUiModel.id) },
                        modifier = Modifier.width(280.dp)
                    )
                }
            }
        }
    }
}