package com.availelabs.availe.domain.extensions

import com.availelabs.availe.domain.model.InteractionState
import com.availelabs.availe.domain.model.Listing

fun Listing.withInteractionState(interactionState: InteractionState): Listing {
    return when (this) {
        is Listing.Event -> this.copy(interactionState = interactionState)
        is Listing.Item -> this.copy(interactionState = interactionState)
        is Listing.Service -> this.copy(interactionState = interactionState)
    }
}