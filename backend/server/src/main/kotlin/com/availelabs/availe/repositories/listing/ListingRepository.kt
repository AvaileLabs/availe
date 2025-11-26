@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.repositories.listing

import com.availelabs.availe.adapters.toDomainListing
import com.availelabs.availe.adapters.toDomainPrice
import com.availelabs.availe.adapters.toMoneta
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.NewListing
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.repository.IListingRepository
import com.availelabs.availe.domain.types.DbDeleteResult
import com.availelabs.availe.domain.types.DbFindResult
import com.availelabs.availe.domain.types.DbInsertResult
import com.availelabs.availe.domain.types.DbUpdateResult
import com.availelabs.availe.dto.PriceDto
import com.availelabs.availe.dto.kreplica.ListingEventDtoSchema
import com.availelabs.availe.dto.kreplica.ListingItemDtoSchema
import com.availelabs.availe.dto.kreplica.ListingServiceDtoSchema
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.repositories.listing.helpers.creators.IListingCreator
import com.example.jooq.tables.references.LISTINGS
import com.example.jooq.tables.references.LISTING_EVENT_DETAILS
import com.example.jooq.tables.references.LISTING_ITEM_DETAILS
import com.example.jooq.tables.references.LISTING_SERVICE_DETAILS
import io.availe.models.Patchable
import org.jooq.DSLContext
import org.jooq.Field
import java.math.BigDecimal
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

internal class ListingRepository(
    private val dsl: DSLContext,
    private val itemCreator: IListingCreator<NewListing.Item>,
    private val eventCreator: IListingCreator<NewListing.Event>,
    private val serviceCreator: IListingCreator<NewListing.Service>
) : IListingRepository {

    private fun baseQuery() = dsl
        .select(LISTINGS.asterisk())
        .from(LISTINGS)
        .leftJoin(LISTING_ITEM_DETAILS).on(LISTINGS.ID.eq(LISTING_ITEM_DETAILS.LISTING_ID))
        .leftJoin(LISTING_EVENT_DETAILS).on(LISTINGS.ID.eq(LISTING_EVENT_DETAILS.LISTING_ID))
        .leftJoin(LISTING_SERVICE_DETAILS).on(LISTINGS.ID.eq(LISTING_SERVICE_DETAILS.LISTING_ID))

    override fun createListing(newListing: NewListing): DbInsertResult<Listing> {
        val newListingId: ListingId = when (newListing) {
            is NewListing.Event -> ListingId(eventCreator.create(newListing))
            is NewListing.Item -> ListingId(itemCreator.create(newListing))
            is NewListing.Service -> ListingId(serviceCreator.create(newListing))
        }

        return when (val findResult = getListingById(newListingId)) {
            is DbFindResult.Found -> DbInsertResult.Inserted(findResult.value)
            is DbFindResult.NotFound -> throw IllegalStateException("Failed to find listing $newListingId immediately after creation")
        }
    }

    override fun getListingById(id: ListingId): DbFindResult<Listing> {
        return baseQuery()
            .where(LISTINGS.ID.eq(id.value.toJavaUuid()))
            .fetchOne()
            ?.let {
                DbFindResult.Found(
                    it.toDomainListing(
                        LISTINGS,
                        LISTING_ITEM_DETAILS,
                        LISTING_EVENT_DETAILS,
                        LISTING_SERVICE_DETAILS
                    )
                )
            }
            ?: DbFindResult.NotFound
    }

    override fun existsById(id: ListingId): Boolean {
        return dsl.fetchExists(
            dsl.selectOne()
                .from(LISTINGS)
                .where(LISTINGS.ID.eq(id.value.toJavaUuid()))
        )
    }

    override fun getAllListings(): List<Listing> {
        return baseQuery()
            .fetch()
            .map {
                it.toDomainListing(
                    LISTINGS,
                    LISTING_ITEM_DETAILS,
                    LISTING_EVENT_DETAILS,
                    LISTING_SERVICE_DETAILS
                )
            }
    }

    private fun addCommonPatchUpdates(
        updates: MutableMap<Field<*>, Any?>,
        title: Patchable<String>,
        description: Patchable<String?>,
        price: Patchable<PriceDto.V1>,
        imageLinks: Patchable<List<String>>
    ) {
        if (title is Patchable.Set) {
            updates[LISTINGS.TITLE] = title.value
        }
        if (description is Patchable.Set) {
            updates[LISTINGS.DESCRIPTION] = description.value
        }
        if (imageLinks is Patchable.Set) {
            updates[LISTINGS.IMAGE_LINKS] = imageLinks.value.toTypedArray()
        }
        if (price is Patchable.Set) {
            val money = price.value.toDomainPrice().toMoneta()
            updates[LISTINGS.PRICE_AMOUNT] = money.number.numberValue(BigDecimal::class.java)
            updates[LISTINGS.PRICE_CURRENCY] = money.currency.currencyCode
        }
    }

    override fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        requestingPublisherId: UserId
    ): DbUpdateResult<Listing> {
        val updates = mutableMapOf<Field<*>, Any?>()

        when (patch) {
            is ListingItemDtoSchema.V1.PatchRequest ->
                addCommonPatchUpdates(updates, patch.title, patch.description, patch.price, patch.imageLinks)

            is ListingEventDtoSchema.V1.PatchRequest ->
                addCommonPatchUpdates(updates, patch.title, patch.description, patch.price, patch.imageLinks)

            is ListingServiceDtoSchema.V1.PatchRequest ->
                addCommonPatchUpdates(updates, patch.title, patch.description, patch.price, patch.imageLinks)
        }

        if (updates.isEmpty()) {
            val isOwner = dsl.fetchExists(
                dsl.selectOne()
                    .from(LISTINGS)
                    .where(LISTINGS.ID.eq(id.value.toJavaUuid()))
                    .and(LISTINGS.PUBLISHER_ID.eq(requestingPublisherId.value.toJavaUuid()))
            )
            return if (isOwner) {
                when (val current = getListingById(id)) {
                    is DbFindResult.Found -> DbUpdateResult.Updated(current.value)
                    is DbFindResult.NotFound -> DbUpdateResult.NotFound
                }
            } else {
                DbUpdateResult.NotFound
            }
        }

        val record = dsl.update(LISTINGS)
            .set(updates)
            .where(LISTINGS.ID.eq(id.value.toJavaUuid()))
            .and(LISTINGS.PUBLISHER_ID.eq(requestingPublisherId.value.toJavaUuid()))
            .returning()
            .fetchOne()

        if (record == null) {
            return DbUpdateResult.NotFound
        }

        return when (val updatedResult = getListingById(id)) {
            is DbFindResult.Found -> DbUpdateResult.Updated(updatedResult.value)
            is DbFindResult.NotFound -> throw IllegalStateException("Failed to find listing $id immediately after update")
        }
    }

    override fun deleteListing(id: ListingId, requestingPublisherId: UserId): DbDeleteResult {
        val rowsAffected = dsl.deleteFrom(LISTINGS)
            .where(LISTINGS.ID.eq(id.value.toJavaUuid()))
            .and(LISTINGS.PUBLISHER_ID.eq(requestingPublisherId.value.toJavaUuid()))
            .execute()

        return if (rowsAffected > 0) DbDeleteResult.Deleted else DbDeleteResult.NotFound
    }
}