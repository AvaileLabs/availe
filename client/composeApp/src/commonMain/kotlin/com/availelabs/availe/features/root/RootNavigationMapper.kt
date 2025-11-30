package com.availelabs.availe.features.root

import com.availelabs.availe.domain.model.ListingId
import io.ktor.http.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object RootNavigationMapper {
    fun getStackForDeepLink(deepLinkUrl: String?): List<Configuration> {
        if (deepLinkUrl == null) {
            return listOf(Configuration.Home)
        }

        return try {
            val uniformResourceLocator = Url(deepLinkUrl)
            val pathSegments = uniformResourceLocator.segments.filter { segment -> segment.isNotEmpty() }

            if (pathSegments.size == 2 && pathSegments[0] == Configuration.ListingDetails.ROUTE_SEGMENT_NAME) {
                val uuidString = pathSegments[1]
                val uuid = Uuid.parse(uuidString)
                val listingId = ListingId(uuid)
                listOf(Configuration.Home, Configuration.ListingDetails(listingId))
            } else {
                listOf(Configuration.Home)
            }
        } catch (_: Exception) {
            listOf(Configuration.Home)
        }
    }

    fun getPathForConfiguration(configuration: Configuration): String {
        return when (configuration) {
            Configuration.Home -> "/"
            is Configuration.ListingDetails -> "/${Configuration.ListingDetails.ROUTE_SEGMENT_NAME}/${configuration.listingId.value}"
        }
    }
}