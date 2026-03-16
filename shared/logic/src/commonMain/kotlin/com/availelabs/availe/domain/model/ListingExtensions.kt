package com.availelabs.availe.domain.model

fun Listing.withInteractionState(interactionState: InteractionState): Listing {
    return when (this) {
        is Listing.Event -> this.copy(interactionState = interactionState)
        is Listing.Item -> this.copy(interactionState = interactionState)
        is Listing.Service -> this.copy(interactionState = interactionState)
    }
}