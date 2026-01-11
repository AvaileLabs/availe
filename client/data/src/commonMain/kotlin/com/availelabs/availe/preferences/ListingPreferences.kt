package com.availelabs.availe.preferences

import com.availelabs.availe.domain.model.ListingCategoryFilter
import com.russhwolf.settings.Settings

internal interface IListingPreferences {
    suspend fun getLastListingsFetchTimeMilliseconds(filter: ListingCategoryFilter): Long?
    suspend fun updateLastListingsFetchTime(timestampMilliseconds: Long, filter: ListingCategoryFilter)
    suspend fun clear()
}

internal class ListingPreferences(
    private val settings: Settings
) : IListingPreferences {

    override suspend fun getLastListingsFetchTimeMilliseconds(filter: ListingCategoryFilter): Long? {
        val key = generateKey(filter)
        if (settings.hasKey(key)) {
            return settings.getLong(key, 0L)
        }
        return null
    }

    override suspend fun updateLastListingsFetchTime(timestampMilliseconds: Long, filter: ListingCategoryFilter) {
        val key = generateKey(filter)
        settings.putLong(key, timestampMilliseconds)
    }

    override suspend fun clear() {
        val keysToRemove = settings.keys.filter { it.startsWith(KEY_PREFIX) }
        keysToRemove.forEach { key ->
            settings.remove(key)
        }
    }

    private fun generateKey(filter: ListingCategoryFilter): String {
        if (filter.isAll) {
            return "${KEY_PREFIX}all"
        }
        val sortedCategories = filter.categories.map { it.name }.sorted().joinToString("_")
        return "${KEY_PREFIX}$sortedCategories"
    }

    companion object {
        private const val KEY_PREFIX = "last_listing_fetch_timestamp_"
    }
}