package com.availelabs.availe.preferences

import com.availelabs.availe.domain.model.EventFeedFilter
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.ListingCategoryFilter
import com.russhwolf.settings.Settings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ListingPreferencesTest {

    @Test
    fun updateAndReadTimestampForAllUpcomingListings() {
        runBlocking {
            val settings = Settings()
            val listingPreferences = ListingPreferences(settings)
            val configuration = ListingFetchConfiguration(
                categoryFilter = ListingCategoryFilter.All,
                eventFeedFilter = EventFeedFilter.Upcoming
            )

            listingPreferences.updateLastListingsFetchTime(
                timestampMilliseconds = 123L,
                configuration = configuration
            )

            val storedTimestamp = listingPreferences.getLastListingsFetchTimeMilliseconds(configuration)

            assertEquals(123L, storedTimestamp)
            listingPreferences.clear()
        }
    }

    @Test
    fun updateAndReadTimestampForSpecificCategories() {
        runBlocking {
            val settings = Settings()
            val listingPreferences = ListingPreferences(settings)
            val configuration = ListingFetchConfiguration(
                categoryFilter = ListingCategoryFilter.of(ListingCategory.Event, ListingCategory.Item),
                eventFeedFilter = EventFeedFilter.None
            )

            listingPreferences.updateLastListingsFetchTime(
                timestampMilliseconds = 456L,
                configuration = configuration
            )

            val storedTimestamp = listingPreferences.getLastListingsFetchTimeMilliseconds(configuration)

            assertEquals(456L, storedTimestamp)
            listingPreferences.clear()
        }
    }
}