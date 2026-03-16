@file:OptIn(ExperimentalUuidApi::class)

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
import com.example.jooq.tables.references.*
import io.availe.models.Patchable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.impl.DSL
import java.math.BigDecimal
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
    private val databaseContext: DSLContext,
    private val itemListingCreator: IListingCreator<NewListing.Item>,
    private val eventListingCreator: IListingCreator<NewListing.Event>,
    private val serviceListingCreator: IListingCreator<NewListing.Service>
) : IListingRepository {

    private fun baseQuery() = databaseContext
        .select(
            LISTINGS.asterisk(),
            USERS.ID,
            USERS.DISPLAY_NAME,
            USERS.AVATAR_URL,
            USERS.JID,
            LISTING_EVENT_DETAILS.START_LOCAL_TIME,
            LISTING_EVENT_DETAILS.END_LOCAL_TIME,
            LISTING_EVENT_DETAILS.START_INSTANT,
            LISTING_EVENT_DETAILS.END_INSTANT,
            LISTING_EVENT_DETAILS.TIME_ZONE_IDENTIFIER
        )
        .from(LISTINGS)
        .leftJoin(USERS).on(LISTINGS.PUBLISHER_ID.eq(USERS.ID))
        .leftJoin(LISTING_ITEM_DETAILS).on(LISTINGS.ID.eq(LISTING_ITEM_DETAILS.LISTING_ID))
        .leftJoin(LISTING_EVENT_DETAILS).on(LISTINGS.ID.eq(LISTING_EVENT_DETAILS.LISTING_ID))
        .leftJoin(LISTING_SERVICE_DETAILS).on(LISTINGS.ID.eq(LISTING_SERVICE_DETAILS.LISTING_ID))

    override fun createListing(newListing: NewListing): InsertResult<Listing> {
        val newListingId: ListingId = when (newListing) {
            is NewListing.Event -> ListingId(eventListingCreator.create(newListing))
            is NewListing.Item -> ListingId(itemListingCreator.create(newListing))
            is NewListing.Service -> ListingId(serviceListingCreator.create(newListing))
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
            ?.toDomainListing(
                LISTINGS,
                USERS,
                LISTING_ITEM_DETAILS,
                LISTING_EVENT_DETAILS,
                LISTING_SERVICE_DETAILS
            )
            ?.let { FindResult.Found(it) }
            ?: FindResult.NotFound
    }

    override fun existsById(id: ListingId): Boolean {
        return databaseContext.fetchExists(
            databaseContext.selectOne()
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
                    USERS,
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
        val offsetAmount = pageNumber * pageSize
        var databaseCondition = DSL.noCondition()

        if (publisherId != null) {
            databaseCondition = databaseCondition.and(LISTINGS.PUBLISHER_ID.eq(publisherId.value.toJavaUuid()))
        }

        if (!filter.isAll) {
            val databaseCategories = filter.categories.map { category ->
                when (category) {
                    ListingCategory.Item -> ListingTypeEnum.item
                    ListingCategory.Event -> ListingTypeEnum.event
                    ListingCategory.Service -> ListingTypeEnum.service
                }
            }
            databaseCondition = databaseCondition.and(LISTINGS.LISTING_TYPE.`in`(databaseCategories))
        }

        val orderByField = LISTINGS.CREATED_AT.desc()

        return baseQuery()
            .where(databaseCondition)
            .orderBy(orderByField)
            .limit(pageSize)
            .offset(offsetAmount)
            .fetch()
            .map {
                it.toDomainListing(
                    LISTINGS,
                    USERS,
                    LISTING_ITEM_DETAILS,
                    LISTING_EVENT_DETAILS,
                    LISTING_SERVICE_DETAILS
                )
            }
    }

    private fun addCommonPatchUpdates(
        databaseUpdates: MutableMap<Field<*>, Any?>,
        titlePatch: Patchable<ListingTitle>,
        descriptionPatch: Patchable<ListingDescription?>,
        pricePatch: Patchable<Price>,
        imageLinksPatch: Patchable<List<ImageUrl>>,
        locationPatch: Patchable<ListingLocation>
    ) {
        if (titlePatch is Patchable.Set) {
            databaseUpdates[LISTINGS.TITLE] = titlePatch.value.value
        }
        if (descriptionPatch is Patchable.Set) {
            databaseUpdates[LISTINGS.DESCRIPTION] = descriptionPatch.value?.value
        }
        if (imageLinksPatch is Patchable.Set) {
            databaseUpdates[LISTINGS.IMAGE_LINKS] = imageLinksPatch.value.map { it.value }.toTypedArray()
        }
        if (pricePatch is Patchable.Set) {
            val monetaMoney = pricePatch.value.toMoneta()
            databaseUpdates[LISTINGS.PRICE_AMOUNT] = monetaMoney.number.numberValue(BigDecimal::class.java)
            databaseUpdates[LISTINGS.PRICE_CURRENCY] = monetaMoney.currency.currencyCode
        }
        if (locationPatch is Patchable.Set) {
            databaseUpdates[LISTINGS.LOCATION] = locationPatch.value.value
        }
    }

    override fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest
    ): UpdateResult<Listing> {
        val databaseUpdates = mutableMapOf<Field<*>, Any?>()

        when (patch) {
            is ListingItemDtoSchema.V1.PatchRequest ->
                addCommonPatchUpdates(
                    databaseUpdates,
                    patch.title,
                    patch.description,
                    patch.price,
                    patch.imageLinks,
                    patch.location
                )

            is ListingEventDtoSchema.V1.PatchRequest ->
                addCommonPatchUpdates(
                    databaseUpdates,
                    patch.title,
                    patch.description,
                    patch.price,
                    patch.imageLinks,
                    patch.location
                )

            is ListingServiceDtoSchema.V1.PatchRequest ->
                addCommonPatchUpdates(
                    databaseUpdates,
                    patch.title,
                    patch.description,
                    patch.price,
                    patch.imageLinks,
                    patch.location
                )
        }

        if (databaseUpdates.isEmpty()) {
            return when (val currentListingResult = getListingById(id)) {
                is FindResult.Found -> UpdateResult.Updated(currentListingResult.value)
                is FindResult.NotFound -> UpdateResult.NotFound
            }
        }

        val updatedRecord = databaseContext.update(LISTINGS)
            .set(databaseUpdates)
            .where(LISTINGS.ID.eq(id.value.toJavaUuid()))
            .returning()
            .fetchOne()

        if (updatedRecord == null) {
            return UpdateResult.NotFound
        }

        return when (val finalListingResult = getListingById(id)) {
            is FindResult.Found -> UpdateResult.Updated(finalListingResult.value)
            is FindResult.NotFound -> throw IllegalStateException("Failed to find listing $id immediately after update")
        }
    }

    override fun deleteListing(id: ListingId): DeleteResult {
        val rowsAffectedCount = databaseContext.deleteFrom(LISTINGS)
            .where(LISTINGS.ID.eq(id.value.toJavaUuid()))
            .execute()

        return if (rowsAffectedCount > 0) DeleteResult.Deleted else DeleteResult.NotFound
    }
}