package com.availelabs.availe.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ListingCategoryFilter(
    val categories: Set<ListingCategory> = emptySet()
) {
    val isAll: Boolean
        get() = categories.isEmpty()

    companion object {
        val All = ListingCategoryFilter()

        fun of(vararg categories: ListingCategory): ListingCategoryFilter {
            return ListingCategoryFilter(categories.toSet())
        }
    }
}