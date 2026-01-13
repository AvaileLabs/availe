package com.availelabs.availe.appshell.model

import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.model.ListingFeedType
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
    data class Home(val feedType: ListingFeedType) : AppShellConfiguration {
        override fun toUrlPath(): String = ""
    }

    @Serializable
    data class Chat(val recipientJid: Jid? = null) : AppShellConfiguration {
        override val canonicalParent: AppShellConfiguration = Dashboard

        override fun toUrlPath(): String =
            if (recipientJid != null) "/$ROUTE_SEGMENT_NAME/${recipientJid.value}" else "/$ROUTE_SEGMENT_NAME"

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

    @Serializable
    data class ListingDetails(val listingId: ListingId) : AppShellConfiguration {
        override val canonicalParent: AppShellConfiguration = Dashboard

        override fun toUrlPath(): String = "/listing/${listingId.value}"
    }

    @Serializable
    data class Profile(val userId: UserId) : AppShellConfiguration {
        override val canonicalParent: AppShellConfiguration = Dashboard

        override fun toUrlPath(): String = "/profile/${userId.value}"
    }

    @Serializable
    data object Settings : AppShellConfiguration {
        override val canonicalParent: AppShellConfiguration = Dashboard

        override fun toUrlPath(): String = "/settings"
    }

    @Serializable
    data class ChatOverlay(val initialJid: Jid? = null) : AppShellConfiguration {
        override fun toUrlPath(): String = ""
    }
}