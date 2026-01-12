@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.root

import com.availelabs.availe.appshell.model.AppShellIntent
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.model.ListingFeedType
import com.availelabs.availe.navigation.DeepLinkRoute
import com.availelabs.availe.navigation.FeedTypeMapper
import io.ktor.http.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object RootNavigationMapper {
    private const val CHAT_QUERY_PARAMETER = "chat_jid"

    fun getInitialIntent(deepLinkUrl: String?): RootConfiguration {
        if (deepLinkUrl == null) {
            return RootConfiguration.AppShell(AppShellIntent.Home(ListingFeedType.Latest))
        }

        return try {
            val url = Url(deepLinkUrl)
            val pathSegments = url.segments.filter { segment -> segment.isNotEmpty() }
            val chatJidValue = url.parameters[CHAT_QUERY_PARAMETER]

            val intent = if (pathSegments.isEmpty()) {
                AppShellIntent.Home(ListingFeedType.Latest)
            } else {
                val rootSegment = pathSegments.first()
                when (rootSegment) {
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
                        val feedType = FeedTypeMapper.fromPathSegment(rootSegment)
                        AppShellIntent.Home(feedType)
                    }
                }
            }

            RootConfiguration.AppShell(intent)
        } catch (_: Exception) {
            RootConfiguration.AppShell(AppShellIntent.Home(ListingFeedType.Latest))
        }
    }

    private fun parseListingIntent(pathSegments: List<String>, chatJidValue: String?): AppShellIntent {
        val listingId = extractUuid(pathSegments, 1)?.let { ListingId(it) }
            ?: return AppShellIntent.Home(ListingFeedType.Latest)

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
        if (pathSegments.size < 2) return AppShellIntent.Home(ListingFeedType.Latest)
        val jidValue = pathSegments[1]
        return AppShellIntent.Chatting(Jid(jidValue))
    }

    private fun parseProfileIntent(pathSegments: List<String>): AppShellIntent {
        val userId = extractUuid(pathSegments, 1)?.let { UserId(it) }
            ?: return AppShellIntent.Home(ListingFeedType.Latest)

        return AppShellIntent.ViewingProfile(userId)
    }

    private fun parseEditListingIntent(pathSegments: List<String>): AppShellIntent {
        val listingId = extractUuid(pathSegments, 1)?.let { ListingId(it) }
            ?: return AppShellIntent.Home(ListingFeedType.Latest)

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