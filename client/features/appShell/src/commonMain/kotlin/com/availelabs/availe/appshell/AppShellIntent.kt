package com.availelabs.availe.appshell

import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppShellIntent {
    @Serializable
    data object Home : AppShellIntent

    @Serializable
    data class ViewingListing(
        val listingId: ListingId
    ) : AppShellIntent

    @Serializable
    data class ViewingListingWithChat(
        val listingId: ListingId,
        val recipientJid: Jid
    ) : AppShellIntent

    @Serializable
    data class Chatting(
        val recipientJid: Jid
    ) : AppShellIntent

    @Serializable
    data class ListingEditor(
        val listingId: ListingId? = null
    ) : AppShellIntent

    @Serializable
    data class ViewingProfile(
        val userId: UserId
    ) : AppShellIntent

    @Serializable
    data object Settings : AppShellIntent
}