package com.availelabs.availe.dashboard

import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.UserId
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@Serializable
sealed interface DashboardConfiguration {
    @OptIn(ExperimentalUuidApi::class)
    fun toUrlPath(): String

    @Serializable
    data object Home : DashboardConfiguration {
        override fun toUrlPath(): String = ""
    }

    @Serializable
    data class Chat(val initialJid: Jid? = null) : DashboardConfiguration {
        override fun toUrlPath(): String = "chat"
    }

    @Serializable
    data class Profile(val userId: UserId) : DashboardConfiguration {
        @OptIn(ExperimentalUuidApi::class)
        override fun toUrlPath(): String = "profile"
    }
}