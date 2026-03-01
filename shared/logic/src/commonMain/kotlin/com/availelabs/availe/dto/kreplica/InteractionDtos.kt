package com.availelabs.availe.dto.kreplica

import com.availelabs.availe.domain.model.InteractionAction
import com.availelabs.availe.domain.model.InteractionType
import io.availe.Replicate
import io.availe.models.DtoVariant
import io.availe.models.DtoVisibility
import kotlinx.serialization.Serializable

@Serializable
sealed interface TypeV1InteractionStateDto

interface InteractionStateDto {
    @Replicate.Model(
        variants = [DtoVariant.DATA],
        visibility = DtoVisibility.PUBLIC
    )
    interface V1 : InteractionStateDto {
        val isFavorited: Boolean
        val isRsvped: Boolean
    }
}

@Serializable
sealed interface TypeV1InteractionRequestDto

interface InteractionRequestDto {
    @Replicate.Model(
        variants = [DtoVariant.DATA],
        visibility = DtoVisibility.PUBLIC
    )
    interface V1 : InteractionRequestDto {
        val action: InteractionAction
        val type: InteractionType
    }
}