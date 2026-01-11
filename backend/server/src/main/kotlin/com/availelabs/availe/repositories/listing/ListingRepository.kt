@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.repositories.listing

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DeleteResult
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.domain.types.InsertResult
import com.availelabs.availe.domain.types.UpdateResult
import com.availelabs.availe.dto.kreplica.ListingEventDtoSchema
import com.availelabs.availe.dto.kreplica.ListingItemDtoSchema
import com.availelabs.availe.dto.kreplica.ListingServiceDtoSchema
import com.availelabs.availe.dto.kreplica.TypeV1ListingDtoPatchRequest
import com.availelabs.availe.mappers.toDomainListing
import com.availelabs.availe.mappers.toMoneta
import com.availelabs.availe.repositories.listing.helpers.creators.IListingCreator
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.references.LISTINGS
import com.example.jooq.tables.references.LISTING_EVENT_DETAILS
import com.example.jooq.tables.references.LISTING_ITEM_DETAILS
import com.example.jooq.tables.references.LISTING_SERVICE_DETAILS
import io.availe.models.Patchable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.impl.DSL
import java.math.BigDecimal
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

interface IListingRepository {
    fun createListing(newListing: NewListing): InsertResult<Listing>
    fun getListingById(id: ListingId): FindResult<Listing>
    fun existsById(id: ListingId): Boolean
    fun getAllListings(): List<Listing>
    fun getListingsPage(
        pageNumber: Int,
        pageSize: Int,
        publisherId: UserId? = null,
        filter: ListingCategoryFilter = ListingCategoryFilter.All
    ): List<Listing>

    fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): UpdateResult<Listing>

    fun deleteListing(id: ListingId): DeleteResult
}

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

    override fun createListing(newListing: NewListing): InsertResult<Listing> {
        val newListingId: ListingId = when (newListing) {
            is NewListing.Event -> ListingId(eventCreator.create(newListing))
            is NewListing.Item -> ListingId(itemCreator.create(newListing))
            is NewListing.Service -> ListingId(serviceCreator.create(newListing))
        }

        return when (val findResult = getListingById(newListingId)) {
            is FindResult.Found -> InsertResult.Inserted(findResult.value)
            is FindResult.NotFound -> throw IllegalStateException("Failed to find listing $newListingId immediately after creation")
        }
    }

    override fun getListingById(id: ListingId): FindResult<Listing> {
        return baseQuery()
            .where(LISTINGS.ID.eq(id.value.toJavaUuid()))
            .fetchOne()
            ?.let {
                FindResult.Found(
                    it.toDomainListing(
                        LISTINGS,
                        LISTING_ITEM_DETAILS,
                        LISTING_EVENT_DETAILS,
                        LISTING_SERVICE_DETAILS
                    )
                )
            }
            ?: FindResult.NotFound
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

    override fun getListingsPage(
        pageNumber: Int,
        pageSize: Int,
        publisherId: UserId?,
        filter: ListingCategoryFilter
    ): List<Listing> {
        val offset = pageNumber * pageSize
        var condition = DSL.noCondition()

        if (publisherId != null) {
            condition = condition.and(LISTINGS.PUBLISHER_ID.eq(publisherId.value.toJavaUuid()))
        }

        if (!filter.isAll) {
            val databaseCategories = filter.categories.map { category ->
                when (category) {
                    ListingCategory.Item -> ListingTypeEnum.item
                    ListingCategory.Event -> ListingTypeEnum.event
                    ListingCategory.Service -> ListingTypeEnum.service
                }
            }
            condition = condition.and(LISTINGS.LISTING_TYPE.`in`(databaseCategories))
        }

        return baseQuery()
            .where(condition)
            .orderBy(LISTINGS.CREATED_AT.desc())
            .limit(pageSize)
            .offset(offset)
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
        title: Patchable<ListingTitle>,
        description: Patchable<ListingDescription?>,
        price: Patchable<Price>,
        imageLinks: Patchable<List<ImageUrl>>
    ) {
        if (title is Patchable.Set) {
            updates[LISTINGS.TITLE] = title.value.value
        }
        if (description is Patchable.Set) {
            updates[LISTINGS.DESCRIPTION] = description.value?.value
        }
        if (imageLinks is Patchable.Set) {
            updates[LISTINGS.IMAGE_LINKS] = imageLinks.value.map { it.value }.toTypedArray()
        }
        if (price is Patchable.Set) {
            val money = price.value.toMoneta()
            updates[LISTINGS.PRICE_AMOUNT] = money.number.numberValue(BigDecimal::class.java)
            updates[LISTINGS.PRICE_CURRENCY] = money.currency.currencyCode
        }
    }

    override fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): UpdateResult<Listing> {
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
            return when (val current = getListingById(id)) {
                is FindResult.Found -> UpdateResult.Updated(current.value)
                is FindResult.NotFound -> UpdateResult.NotFound
            }
        }

        val record = dsl.update(LISTINGS)
            .set(updates)
            .where(LISTINGS.ID.eq(id.value.toJavaUuid()))
            .returning()
            .fetchOne()

        if (record == null) {
            return UpdateResult.NotFound
        }

        return when (val updatedResult = getListingById(id)) {
            is FindResult.Found -> UpdateResult.Updated(updatedResult.value)
            is FindResult.NotFound -> throw IllegalStateException("Failed to find listing $id immediately after update")
        }
    }

    override fun deleteListing(id: ListingId): DeleteResult {
        val rowsAffected = dsl.deleteFrom(LISTINGS)
            .where(LISTINGS.ID.eq(id.value.toJavaUuid()))
            .execute()

        return if (rowsAffected > 0) DeleteResult.Deleted else DeleteResult.NotFound
    }
}