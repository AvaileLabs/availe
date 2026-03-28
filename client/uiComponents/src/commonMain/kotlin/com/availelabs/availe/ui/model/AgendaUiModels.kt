package com.availelabs.availe.ui.model

import com.availelabs.availe.domain.model.ImageUrl
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.ListingTitle
import kotlin.time.Instant

sealed interface EventProximity {
    data object Ongoing : EventProximity
    data object Today : EventProximity
    data object Tomorrow : EventProximity
    data class Future(val daysAway: Int) : EventProximity {
        init {
            require(daysAway >= 2) { "Days away must be 2 or greater." }
        }
    }
}

data class AgendaUiModel(
    val id: ListingId,
    val title: ListingTitle,
    val imageUrl: ImageUrl?,
    val startTime: Instant,
    val proximity: EventProximity
)

sealed interface AgendaState {
    data object Hidden : AgendaState
    data class Visible(val items: List<AgendaUiModel>) : AgendaState {
        init {
            require(items.isNotEmpty()) { "Agenda carousel must contain at least one item." }
        }
    }
}