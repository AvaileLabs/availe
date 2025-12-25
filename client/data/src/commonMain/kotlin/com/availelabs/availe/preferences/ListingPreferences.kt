package com.availelabs.availe.preferences

import com.russhwolf.settings.Settings

internal interface IListingPreferences {
    suspend fun getLastListingsFetchTimeMilliseconds(): Long?
    suspend fun updateLastListingsFetchTime(timestampMilliseconds: Long)
    suspend fun clear()
}

internal class ListingPreferences(
    private val settings: Settings
) : IListingPreferences {

    override suspend fun getLastListingsFetchTimeMilliseconds(): Long? {
        if (settings.hasKey(KEY_LAST_LISTING_FETCH_TIMESTAMP)) {
            return settings.getLong(KEY_LAST_LISTING_FETCH_TIMESTAMP, 0L)
        }
        return null
    }

    override suspend fun updateLastListingsFetchTime(timestampMilliseconds: Long) {
        settings.putLong(KEY_LAST_LISTING_FETCH_TIMESTAMP, timestampMilliseconds)
    }

    override suspend fun clear() {
        settings.remove(KEY_LAST_LISTING_FETCH_TIMESTAMP)
    }

    companion object {
        private const val KEY_LAST_LISTING_FETCH_TIMESTAMP = "last_listing_fetch_timestamp"
    }
}