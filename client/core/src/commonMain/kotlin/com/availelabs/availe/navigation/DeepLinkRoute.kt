package com.availelabs.availe.navigation

import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
sealed interface DeepLinkRoute {
    val relativePath: String

    data object Home : DeepLinkRoute {
        override val relativePath: String = ""
    }

    data class ListingDetails(val id: ListingId) : DeepLinkRoute {
        override val relativePath: String = "$PATH/${id.value}"

        companion object {
            const val PATH = "listing"
        }
    }

    data class Chat(val jid: Jid? = null) : DeepLinkRoute {
        override val relativePath: String = if (jid != null) "$PATH/${jid.value}" else PATH

        companion object {
            const val PATH = "chat"
        }
    }

    data class Profile(val id: UserId) : DeepLinkRoute {
        override val relativePath: String = "$PATH/${id.value}"

        companion object {
            const val PATH = "profile"
        }
    }

    data object Settings : DeepLinkRoute {
        const val PATH = "settings"
        override val relativePath: String = PATH
    }

    data object CreateListing : DeepLinkRoute {
        const val PATH = "create-listing"
        override val relativePath: String = PATH
    }

    data class EditListing(val id: ListingId) : DeepLinkRoute {
        override val relativePath: String = "$PATH/${id.value}"

        companion object {
            const val PATH = "edit-listing"
        }
    }
}