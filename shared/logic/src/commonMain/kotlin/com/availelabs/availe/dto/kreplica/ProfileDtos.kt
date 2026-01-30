package com.availelabs.availe.dto.kreplica

import com.availelabs.availe.domain.model.ImageUrl
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.UserDisplayName
import com.availelabs.availe.domain.model.UserId
import io.availe.Replicate
import io.availe.models.DtoVariant
import io.availe.models.DtoVisibility
import kotlinx.serialization.Serializable

@Serializable
sealed interface TypeV1PublicProfileDto

interface PublicProfileDto {
    @Replicate.Model(
        variants = [DtoVariant.DATA],
        visibility = DtoVisibility.PUBLIC
    )
    interface V1 : PublicProfileDto {
        val id: UserId
        val displayName: UserDisplayName
        val avatarUrl: ImageUrl?
        val jid: Jid
    }
}