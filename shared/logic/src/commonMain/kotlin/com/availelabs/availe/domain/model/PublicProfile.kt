package com.availelabs.availe.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PublicProfile(
    val id: UserId,
    val displayName: UserDisplayName,
    val avatarUrl: ImageUrl?,
    val jid: Jid
)