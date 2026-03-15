package com.availelabs.availe.preferences

import com.availelabs.availe.domain.model.EventFeedFilter
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.ListingCategoryFilter
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class ListingFetchConfiguration(
    val categoryFilter: ListingCategoryFilter,
    val eventFeedFilter: EventFeedFilter
)

@JvmInline
value class PreferenceKey(val value: String)

internal interface IListingPreferences {
    suspend fun getLastListingsFetchTimeMilliseconds(configuration: ListingFetchConfiguration): Long?
    suspend fun updateLastListingsFetchTime(timestampMilliseconds: Long, configuration: ListingFetchConfiguration)
    suspend fun clear()
    suspend fun saveDraft(draftJsonString: String)
    suspend fun getDraft(): String?
    suspend fun clearDraft()
}

internal class ListingPreferences(
    private val settings: Settings
) : IListingPreferences {

    override suspend fun getLastListingsFetchTimeMilliseconds(configuration: ListingFetchConfiguration): Long? {
        val preferenceKey = generateKey(configuration)
        if (settings.hasKey(preferenceKey.value)) {
            return settings.getLong(preferenceKey.value, 0L)
        }
        return null
    }

    override suspend fun updateLastListingsFetchTime(
        timestampMilliseconds: Long,
        configuration: ListingFetchConfiguration
    ) {
        val preferenceKey = generateKey(configuration)
        settings.putLong(preferenceKey.value, timestampMilliseconds)
    }

    override suspend fun clear() {
        val keysToRemove = settings.keys.filter { it.startsWith(KEY_PREFIX) }
        keysToRemove.forEach { keyToRemove ->
            settings.remove(keyToRemove)
        }
        clearDraft()
    }

    override suspend fun saveDraft(draftJsonString: String) {
        settings.putString(KEY_DRAFT, draftJsonString)
    }

    override suspend fun getDraft(): String? {
        return if (settings.hasKey(KEY_DRAFT)) {
            settings.getString(KEY_DRAFT, "")
        } else {
            null
        }
    }

    override suspend fun clearDraft() {
        if (settings.hasKey(KEY_DRAFT)) {
            settings.remove(KEY_DRAFT)
        }
    }

    private fun generateKey(configuration: ListingFetchConfiguration): PreferenceKey {
        return PreferenceKey(
            buildString {
                append(KEY_PREFIX)
                append(configuration.categoryFilter.toPreferenceSegment())
                append(KEY_DELIMITER)
                append(configuration.eventFeedFilter.name)
            }
        )
    }

    private fun ListingCategoryFilter.toPreferenceSegment(): String {
        if (isAll) {
            return ALL_CATEGORIES_SEGMENT
        }

        return categories
            .sortedBy(ListingCategory::name)
            .joinToString(separator = CATEGORY_DELIMITER) { category -> category.name }
    }

    companion object {
        private const val KEY_PREFIX = "last_listing_fetch_timestamp_"
        private const val KEY_DRAFT = "listing_editor_draft"
        private const val KEY_DELIMITER = "|"
        private const val CATEGORY_DELIMITER = ","
        private const val ALL_CATEGORIES_SEGMENT = "ALL"
    }
}