package com.availelabs.availe.dto.kreplica

import com.availelabs.availe.dto.PriceDto
import io.availe.Replicate
import io.availe.models.DtoVariant
import io.availe.models.DtoVisibility
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
sealed interface TypeV1ListingDto
private interface ListingDto {
    @OptIn(ExperimentalUuidApi::class)
    @Replicate.Model(
        variants = [DtoVariant.DATA, DtoVariant.PATCH, DtoVariant.CREATE],
        visibility = DtoVisibility.INTERNAL,
    )
    interface V1 : ListingDto {
        @Replicate.Property(include = [DtoVariant.DATA])
        val id: Uuid
        val title: String
        val description: String?
        val price: PriceDto.V1
        val imageLinks: List<String>
    }
}

private interface ListingItemDto {
    @Replicate.Model(
        variants = [DtoVariant.DATA, DtoVariant.PATCH, DtoVariant.CREATE],
        supertypes = [TypeV1ListingDto::class]
    )
    @Replicate.Apply([Serializable::class])
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
    @Replicate.Apply([Serializable::class])
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
    @Replicate.Apply([Serializable::class])
    interface V1 : ListingServiceDto {
        @Replicate.Flatten
        val commonData: ListingDtoSchema.V1
    }
}