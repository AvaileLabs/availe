package com.availelabs.availe.repositories

import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.model.EventFeedQuery
import com.availelabs.availe.types.PageSyncResult
import kotlinx.coroutines.flow.Flow

interface IEventRepository {
    fun observeEventFeed(
        limit: Long,
        publisherId: UserId? = null,
        query: EventFeedQuery
    ): Flow<List<Listing.Event>>

    suspend fun syncEventFeedPage(
        page: Int,
        size: Int,
        publisherId: UserId? = null,
        query: EventFeedQuery
    ): PageSyncResult

    suspend fun isEventCacheStale(query: EventFeedQuery): Boolean
}