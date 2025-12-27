@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.appshell

import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@Serializable
sealed interface AppShellConfiguration {
    val canonicalParent: AppShellConfiguration? get() = null

    fun toUrlPath(): String

    @Serializable
    data object Home : AppShellConfiguration {
        override fun toUrlPath(): String = "/"
    }

    @Serializable
    data class ListingDetails(val listingId: ListingId) : AppShellConfiguration {
        override val canonicalParent: AppShellConfiguration = Home

        override fun toUrlPath(): String = "/$ROUTE_SEGMENT_NAME/${listingId.value}"

        companion object {
            const val ROUTE_SEGMENT_NAME = "listing"
        }
    }

    @Serializable
    data class Chat(val recipientJid: Jid) : AppShellConfiguration {
        override val canonicalParent: AppShellConfiguration = Home

        override fun toUrlPath(): String = "/$ROUTE_SEGMENT_NAME/${recipientJid.value}"

        companion object {
            const val ROUTE_SEGMENT_NAME = "chat"
        }
    }

    @Serializable
    data object Settings : AppShellConfiguration {
        override fun toUrlPath(): String = "/settings"
    }

    @Serializable
    data class ListingEditor(val listingId: ListingId?) : AppShellConfiguration {
        override val canonicalParent: AppShellConfiguration = Home

        override fun toUrlPath(): String =
            if (listingId == null) "/create-listing" else "/edit-listing/${listingId.value}"
    }

    @Serializable
    data class Profile(val userId: UserId) : AppShellConfiguration {
        override val canonicalParent: AppShellConfiguration = Home

        override fun toUrlPath(): String = "/$ROUTE_SEGMENT_NAME/${userId.value}"

        companion object {
            const val ROUTE_SEGMENT_NAME = "profile"
        }
    }
}