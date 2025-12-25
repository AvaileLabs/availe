@file:OptIn(ExperimentalStoreApi::class)

package com.availelabs.availe.model

import kotlinx.serialization.Serializable
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.core5.InsertionStrategy
import org.mobilenativefoundation.store.core5.StoreKey

@Serializable
data class ListingStoreKey(
    override val page: Int,
    override val size: Int,
    override val sort: StoreKey.Sort? = null,
    override val filters: List<StoreKey.Filter<*>>? = null
) : StoreKey.Collection.Page {
    override val insertionStrategy: InsertionStrategy = InsertionStrategy.APPEND
}