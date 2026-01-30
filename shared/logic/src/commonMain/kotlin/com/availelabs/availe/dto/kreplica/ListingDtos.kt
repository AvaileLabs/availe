package com.availelabs.availe.dto.kreplica

import com.availelabs.availe.domain.model.*
import io.availe.Replicate
import io.availe.models.DtoVariant
import io.availe.models.DtoVisibility
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@Serializable
sealed interface TypeV1ListingDto

private interface ListingDto {
    @OptIn(ExperimentalUuidApi::class)
    @Replicate.Model(
        variants = [DtoVariant.DATA, DtoVariant.PATCH, DtoVariant.CREATE],
        visibility = DtoVisibility.PUBLIC,
    )
    interface V1 : ListingDto {
        @Replicate.Property(include = [DtoVariant.DATA])
        val id: ListingId
        val title: ListingTitle
        val description: ListingDescription?
        val price: Price
        val imageLinks: List<ImageUrl>
        val location: ListingLocation

        @Replicate.Property(include = [DtoVariant.DATA])
        val publisher: PublicProfileDtoSchema.V1.Data

        @Replicate.Property(include = [DtoVariant.DATA])
        val interactionState: InteractionStateDtoSchema.V1.Data
    }
}

private interface ListingItemDto {
    @Replicate.Model(
        variants = [DtoVariant.DATA, DtoVariant.PATCH, DtoVariant.CREATE],
        supertypes = [TypeV1ListingDto::class]
    )
    interface V1 : ListingItemDto {
        @Replicate.Flatten
        val commonData: ListingDtoSchema.V1
    }
}

private interface ListingEventDto {
    @Replicate.Model(
        variants = [DtoVariant.DATA, DtoVariant.PATCH, DtoVariant.CREATE],
        supertypes = [TypeV1ListingDto::class]
    )
    interface V1 : ListingEventDto {
        @Replicate.Flatten
        val commonData: ListingDtoSchema.V1
    }
}

private interface ListingServiceDto {
    @Replicate.Model(
        variants = [DtoVariant.DATA, DtoVariant.PATCH, DtoVariant.CREATE],
        supertypes = [TypeV1ListingDto::class]
    )
    interface V1 : ListingServiceDto {
        @Replicate.Flatten
        val commonData: ListingDtoSchema.V1
    }
}