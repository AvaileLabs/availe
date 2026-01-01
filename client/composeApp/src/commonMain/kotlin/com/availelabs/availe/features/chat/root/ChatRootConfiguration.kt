package com.availelabs.availe.features.chat.root

import com.availelabs.availe.domain.model.Jid
import kotlinx.serialization.Serializable

@Serializable
sealed interface ChatRootConfiguration {
    @Serializable
    data object List : ChatRootConfiguration

    @Serializable
    data class Details(val jid: Jid) : ChatRootConfiguration
}