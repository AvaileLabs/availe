package com.availelabs.availe.features.root

import com.availelabs.availe.domain.model.Jid
import io.ktor.http.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object RootNavigationMapper {
    private val routeParsers: Map<String, Configuration.RouteParser<*>> = mapOf(
        Configuration.ListingDetails.ROUTE_SEGMENT_NAME to Configuration.ListingDetails
    )

    fun getStackForDeepLink(deepLinkUrl: String?): List<Configuration> {
        if (deepLinkUrl == null) {
            return listOf(Configuration.Home)
        }

        return try {
            val uniformResourceLocator = Url(deepLinkUrl)
            val pathSegments = uniformResourceLocator.segments.filter { segment -> segment.isNotEmpty() }

            if (pathSegments.isEmpty()) {
                return listOf(Configuration.Home)
            }

            val rootSegment = pathSegments.first()
            val parser = routeParsers[rootSegment]

            if (parser != null) {
                val configuration = parser.parse(pathSegments.drop(1))
                if (configuration != null) {
                    val stack = mutableListOf<Configuration>()
                    var current: Configuration? = configuration
                    while (current != null) {
                        stack.add(0, current)
                        current = current.canonicalParent
                    }
                    return stack
                }
            } else if (rootSegment == "chat" && pathSegments.size > 1) {
                val jidValue = pathSegments[1]
                return listOf(Configuration.Home, Configuration.Chat(Jid(jidValue)))
            }

            listOf(Configuration.Home)
        } catch (_: Exception) {
            listOf(Configuration.Home)
        }
    }

    fun getPathForConfiguration(configuration: Configuration): String {
        return configuration.toUrlPath()
    }
}