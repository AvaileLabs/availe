package com.availelabs.availe.features.appshell

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
}