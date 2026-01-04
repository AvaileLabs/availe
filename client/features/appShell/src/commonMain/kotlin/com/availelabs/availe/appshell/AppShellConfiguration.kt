package com.availelabs.availe.appshell

import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Serializable
sealed interface AppShellConfiguration {
    val canonicalParent: AppShellConfiguration? get() = null

    fun toUrlPath(): String

    @Serializable
    data object Dashboard : AppShellConfiguration {
        override fun toUrlPath(): String = ""
    }

    @Serializable
    data class Chat(val recipientJid: Jid) : AppShellConfiguration {
        override val canonicalParent: AppShellConfiguration = Dashboard

        override fun toUrlPath(): String = "/$ROUTE_SEGMENT_NAME/${recipientJid.value}"

        companion object {
            const val ROUTE_SEGMENT_NAME = "chat"
        }
    }

    @Serializable
    data class ListingEditor(val listingId: ListingId?) : AppShellConfiguration {
        override val canonicalParent: AppShellConfiguration = Dashboard

        override fun toUrlPath(): String =
            if (listingId == null) "/create-listing" else "/edit-listing/${listingId.value}"
    }

    // Configuration for the persistent overlay slot.
    // Now holds optional Jid to restore conversation state when switching layout modes.
    @Serializable
    data class ChatOverlay(val initialJid: Jid? = null) : AppShellConfiguration {
        override fun toUrlPath(): String = ""
    }
}