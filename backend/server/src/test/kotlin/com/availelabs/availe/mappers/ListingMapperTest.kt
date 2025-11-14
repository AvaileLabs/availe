@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.availelabs.availe.mappers

import com.availelabs.availe.domain.listing.CommonListingData
import com.availelabs.availe.domain.listing.Listing
import com.example.jooq.enums.ListingTypeEnum
import com.example.jooq.tables.records.ListingsRecord
import org.javamoney.moneta.Money
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.reflect.full.memberProperties
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private val TEST_ID = Uuid.parse("11111111-1111-1111-1111-111111111111")
private val TEST_PUBLISHER_ID = Uuid.parse("22222222-2222-2222-2222-222222222222")
private const val TEST_TITLE = "Test Bike"
private const val TEST_DESCRIPTION = "A blue test bike"
private val TEST_PRICE = Money.of(123.45, "USD")
private val TEST_IMAGE_LINKS = listOf("image1.png", "image2.png")
private val TEST_CREATED_AT_INSTANT = Instant.parse("2025-10-25T12:00:00Z")
private val TEST_CREATED_AT_OFFSETDATETIME = OffsetDateTime.ofInstant(TEST_CREATED_AT_INSTANT, ZoneOffset.UTC)

private fun aNewCommonData(): CommonListingData {
    return CommonListingData(
        title = TEST_TITLE,
        description = TEST_DESCRIPTION,
        price = TEST_PRICE,
        imageLinks = TEST_IMAGE_LINKS,
        publisherId = TEST_PUBLISHER_ID
    )
}

private fun aListingsRecord(type: ListingTypeEnum): ListingsRecord {
    return ListingsRecord().apply {
        id = TEST_ID.toJavaUuid()
        createdAt = TEST_CREATED_AT_OFFSETDATETIME
        publisherId = TEST_PUBLISHER_ID.toJavaUuid()
        listingType = type
        title = TEST_TITLE
        description = TEST_DESCRIPTION
        priceAmount = TEST_PRICE.number.numberValue(BigDecimal::class.java)
        priceCurrency = TEST_PRICE.currency.currencyCode
        imageLinks = TEST_IMAGE_LINKS.toTypedArray()
    }
}

class ListingMapperTest {

    @Test
    fun `applyCommonData fully maps all properties from CommonListingData`() {
        val commonData = aNewCommonData()
        val record = ListingsRecord()
        val testedProperties = mutableSetOf<String>()

        record.applyCommonData(commonData)

        assertEquals(commonData.title, record.title)
        testedProperties.add("title")

        assertEquals(commonData.description, record.description)
        testedProperties.add("description")

        assertEquals(commonData.publisherId.toJavaUuid(), record.publisherId)
        testedProperties.add("publisherId")

        assertArrayEquals(commonData.imageLinks.toTypedArray(), record.imageLinks)
        testedProperties.add("imageLinks")

        assertEquals(commonData.price.number.numberValue(BigDecimal::class.java), record.priceAmount)
        assertEquals(commonData.price.currency.currencyCode, record.priceCurrency)
        testedProperties.add("price")

        val allDomainProperties = CommonListingData::class.memberProperties.map { it.name }.toSet()
        assertEquals(
            allDomainProperties,
            testedProperties,
            "A property was added to CommonListingData but is not being mapped or tested in applyCommonData."
        )
    }

    @Test
    fun `toDomainListing maps correctly to Item`() {
        val record = aListingsRecord(ListingTypeEnum.item)
        val expectedCommonData = aNewCommonData()

        val expectedListing = Listing.Item(
            commonData = expectedCommonData,
            id = TEST_ID,
            createdAt = TEST_CREATED_AT_INSTANT.toKotlinInstant()
        )

        val actualListing = record.toDomainListing()

        assertEquals(expectedListing, actualListing)
    }

    @Test
    fun `toDomainListing maps correctly to Event`() {
        val record = aListingsRecord(ListingTypeEnum.event)
        val expectedCommonData = aNewCommonData()

        val expectedListing = Listing.Event(
            commonData = expectedCommonData,
            id = TEST_ID,
            createdAt = TEST_CREATED_AT_INSTANT.toKotlinInstant()
        )

        val actualListing = record.toDomainListing()

        assertEquals(expectedListing, actualListing)
    }

    @Test
    fun `toDomainListing maps correctly to Service`() {
        val record = aListingsRecord(ListingTypeEnum.service)
        val expectedCommonData = aNewCommonData()

        val expectedListing = Listing.Service(
            commonData = expectedCommonData,
            id = TEST_ID,
            createdAt = TEST_CREATED_AT_INSTANT.toKotlinInstant()
        )

        val actualListing = record.toDomainListing()

        assertEquals(expectedListing, actualListing)
    }

    @Test
    fun `toDomainListing maps null description and empty imageLinks`() {
        val record = aListingsRecord(ListingTypeEnum.item).apply {
            description = null
            imageLinks = arrayOf()
        }

        val actualListing = record.toDomainListing()

        assertEquals(null, actualListing.description)
        assertEquals(emptyList<String>(), actualListing.imageLinks)
    }
}