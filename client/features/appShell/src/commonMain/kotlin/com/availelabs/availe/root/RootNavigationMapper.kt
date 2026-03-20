@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.root

import com.availelabs.availe.appshell.model.AppShellIntent
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.logging.LogCategory
import com.availelabs.availe.logging.logger
import com.availelabs.availe.model.ListingFeedType
import com.availelabs.availe.navigation.DeepLinkRoute
import com.availelabs.availe.navigation.FeedTypeMapper
import io.ktor.http.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object RootNavigationMapper {
    private const val CHAT_QUERY_PARAMETER = "chat_jid"

    private val classLogger by logger(LogCategory.ROUTING)

    fun getInitialIntent(deepLinkUrl: String?): RootConfiguration {
        classLogger.d { "Parsing deep link url: $deepLinkUrl" }

        if (deepLinkUrl == null) {
            return RootConfiguration.AppShell(AppShellIntent.Home(ListingFeedType.Upcoming))
        }

        return try {
            val parsedUrl = Url(deepLinkUrl)
            val pathSegments = parsedUrl.segments.filter { segment -> segment.isNotEmpty() }
            val chatJidValue = parsedUrl.parameters[CHAT_QUERY_PARAMETER]

            val intent = if (pathSegments.isEmpty()) {
                AppShellIntent.Home(ListingFeedType.Upcoming)
            } else {
                when (val rootSegment = pathSegments.first()) {
                    DeepLinkRoute.ListingDetails.PATH -> {
                        parseListingIntent(pathSegments, chatJidValue)
                    }

                    DeepLinkRoute.Chat.PATH -> {
                        parseChatIntent(pathSegments)
                    }

                    DeepLinkRoute.Profile.PATH -> {
                        parseProfileIntent(pathSegments)
                    }

                    DeepLinkRoute.Settings.PATH -> {
                        AppShellIntent.Settings
                    }

                    DeepLinkRoute.CreateListing.PATH -> {
                        AppShellIntent.ListingEditor(null)
                    }

                    DeepLinkRoute.EditListing.PATH -> {
                        parseEditListingIntent(pathSegments)
                    }

                    else -> {
                        classLogger.w { "Unknown deep link root segment: $rootSegment, falling back to FeedType mapper." }
                        val feedType = FeedTypeMapper.fromPathSegment(rootSegment)
                        AppShellIntent.Home(feedType)
                    }
                }
            }

            classLogger.d { "Deep link parsed successfully to intent: ${intent::class.simpleName}" }

            RootConfiguration.AppShell(intent)
        } catch (exception: Exception) {
            classLogger.w(exception) { "Failed to parse deep link url, falling back to default Home intent." }
            RootConfiguration.AppShell(AppShellIntent.Home(ListingFeedType.Upcoming))
        }
    }

    private fun parseListingIntent(pathSegments: List<String>, chatJidValue: String?): AppShellIntent {
        val listingId = extractUuid(pathSegments, 1)?.let { ListingId(it) }
            ?: return AppShellIntent.Home(ListingFeedType.Upcoming)

        return if (!chatJidValue.isNullOrBlank()) {
            AppShellIntent.ViewingListingWithChat(
                listingId = listingId,
                recipientJid = Jid(chatJidValue)
            )
        } else {
            AppShellIntent.ViewingListing(listingId)
        }
    }

    private fun parseChatIntent(pathSegments: List<String>): AppShellIntent {
        if (pathSegments.size < 2) return AppShellIntent.Home(ListingFeedType.Upcoming)
        val jidValue = pathSegments[1]
        return AppShellIntent.Chatting(Jid(jidValue))
    }

    private fun parseProfileIntent(pathSegments: List<String>): AppShellIntent {
        val userId = extractUuid(pathSegments, 1)?.let { UserId(it) }
            ?: return AppShellIntent.Home(ListingFeedType.Upcoming)

        return AppShellIntent.ViewingProfile(userId)
    }

    private fun parseEditListingIntent(pathSegments: List<String>): AppShellIntent {
        val listingId = extractUuid(pathSegments, 1)?.let { ListingId(it) }
            ?: return AppShellIntent.Home(ListingFeedType.Upcoming)

        return AppShellIntent.ListingEditor(listingId)
    }

    private fun extractUuid(pathSegments: List<String>, index: Int): Uuid? {
        if (index >= pathSegments.size) return null
        return try {
            Uuid.parse(pathSegments[index])
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}