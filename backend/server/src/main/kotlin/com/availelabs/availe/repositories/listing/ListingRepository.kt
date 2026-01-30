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
import com.example.jooq.enums.InteractionTypeEnum
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.references.*
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
    fun getListingById(id: ListingId, viewingUserId: UserId?): FindResult<Listing>
    fun existsById(id: ListingId): Boolean
    fun getAllListings(viewingUserId: UserId?): List<Listing>
    fun getListingsPage(
        pageNumber: Int,
        pageSize: Int,
        viewingUserId: UserId?,
        publisherId: UserId? = null,
        filter: ListingCategoryFilter = ListingCategoryFilter.All
    ): List<Listing>

    fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        viewingUserId: UserId
    ): UpdateResult<Listing>

    fun deleteListing(id: ListingId): DeleteResult
}

internal class ListingRepository(
    private val dsl: DSLContext,
    private val itemCreator: IListingCreator<NewListing.Item>,
    private val eventCreator: IListingCreator<NewListing.Event>,
    private val serviceCreator: IListingCreator<NewListing.Service>
) : IListingRepository {

    private fun baseQuery(viewingUserId: UserId?) = dsl
        .select(
            LISTINGS.asterisk(),
            USERS.ID,
            USERS.DISPLAY_NAME,
            USERS.AVATAR_URL,
            USERS.JID,
            if (viewingUserId != null) {
                DSL.field(
                    DSL.exists(
                        DSL.selectOne()
                            .from(LISTING_INTERACTIONS)
                            .where(LISTING_INTERACTIONS.LISTING_ID.eq(LISTINGS.ID))
                            .and(LISTING_INTERACTIONS.USER_ID.eq(viewingUserId.value.toJavaUuid()))
                            .and(LISTING_INTERACTIONS.INTERACTION_TYPE.eq(InteractionTypeEnum.favorite))
                    )
                ).`as`("is_favorited")
            } else {
                DSL.inline(false).`as`("is_favorited")
            }
        )
        .from(LISTINGS)
        .leftJoin(USERS).on(LISTINGS.PUBLISHER_ID.eq(USERS.ID))
        .leftJoin(LISTING_ITEM_DETAILS).on(LISTINGS.ID.eq(LISTING_ITEM_DETAILS.LISTING_ID))
        .leftJoin(LISTING_EVENT_DETAILS).on(LISTINGS.ID.eq(LISTING_EVENT_DETAILS.LISTING_ID))
        .leftJoin(LISTING_SERVICE_DETAILS).on(LISTINGS.ID.eq(LISTING_SERVICE_DETAILS.LISTING_ID))

    override fun createListing(newListing: NewListing): InsertResult<Listing> {
        val newListingId: ListingId = when (newListing) {
            is NewListing.Event -> ListingId(eventCreator.create(newListing))
            is NewListing.Item -> ListingId(itemCreator.create(newListing))
            is NewListing.Service -> ListingId(serviceCreator.create(newListing))
        }

        return when (val findResult = getListingById(newListingId, newListing.publisher.id)) {
            is FindResult.Found -> InsertResult.Inserted(findResult.value)
            is FindResult.NotFound -> throw IllegalStateException("Failed to find listing $newListingId immediately after creation")
        }
    }

    override fun getListingById(id: ListingId, viewingUserId: UserId?): FindResult<Listing> {
        return baseQuery(viewingUserId)
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
        return dsl.fetchExists(
            dsl.selectOne()
                .from(LISTINGS)
                .where(LISTINGS.ID.eq(id.value.toJavaUuid()))
        )
    }

    override fun getAllListings(viewingUserId: UserId?): List<Listing> {
        return baseQuery(viewingUserId)
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
        viewingUserId: UserId?,
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

        return baseQuery(viewingUserId)
            .where(condition)
            .orderBy(LISTINGS.CREATED_AT.desc())
            .limit(pageSize)
            .offset(offset)
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
        updates: MutableMap<Field<*>, Any?>,
        title: Patchable<ListingTitle>,
        description: Patchable<ListingDescription?>,
        price: Patchable<Price>,
        imageLinks: Patchable<List<ImageUrl>>,
        location: Patchable<ListingLocation>
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
        if (location is Patchable.Set) {
            updates[LISTINGS.LOCATION] = location.value.value
        }
    }

    override fun updateListing(
        id: ListingId,
        patch: TypeV1ListingDtoPatchRequest,
        viewingUserId: UserId
    ): UpdateResult<Listing> {
        val updates = mutableMapOf<Field<*>, Any?>()

        when (patch) {
            is ListingItemDtoSchema.V1.PatchRequest ->
                addCommonPatchUpdates(
                    updates,
                    patch.title,
                    patch.description,
                    patch.price,
                    patch.imageLinks,
                    patch.location
                )

            is ListingEventDtoSchema.V1.PatchRequest ->
                addCommonPatchUpdates(
                    updates,
                    patch.title,
                    patch.description,
                    patch.price,
                    patch.imageLinks,
                    patch.location
                )

            is ListingServiceDtoSchema.V1.PatchRequest ->
                addCommonPatchUpdates(
                    updates,
                    patch.title,
                    patch.description,
                    patch.price,
                    patch.imageLinks,
                    patch.location
                )
        }

        if (updates.isEmpty()) {
            return when (val current = getListingById(id, viewingUserId)) {
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

        return when (val updatedResult = getListingById(id, viewingUserId)) {
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