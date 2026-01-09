@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.root

import com.availelabs.availe.appshell.model.AppShellIntent
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.navigation.DeepLinkRoute
import io.ktor.http.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object RootNavigationMapper {
    private const val CHAT_QUERY_PARAMETER = "chat_jid"

    fun getInitialIntent(deepLinkUrl: String?): RootConfiguration {
        if (deepLinkUrl == null) {
            return RootConfiguration.AppShell(AppShellIntent.Home)
        }

        return try {
            val url = Url(deepLinkUrl)
            val pathSegments = url.segments.filter { segment -> segment.isNotEmpty() }
            val chatJidValue = url.parameters[CHAT_QUERY_PARAMETER]

            val intent = if (pathSegments.isEmpty()) {
                AppShellIntent.Home
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

                    else -> AppShellIntent.Home
                }
            }

            RootConfiguration.AppShell(intent)
        } catch (_: Exception) {
            RootConfiguration.AppShell(AppShellIntent.Home)
        }
    }

    private fun parseListingIntent(pathSegments: List<String>, chatJidValue: String?): AppShellIntent {
        if (pathSegments.size < 2) return AppShellIntent.Home

        val uuidString = pathSegments[1]
        val listingId = try {
            ListingId(Uuid.parse(uuidString))
        } catch (_: IllegalArgumentException) {
            return AppShellIntent.Home
        }

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
        if (pathSegments.size < 2) return AppShellIntent.Home
        val jidValue = pathSegments[1]
        return AppShellIntent.Chatting(Jid(jidValue))
    }

    private fun parseProfileIntent(pathSegments: List<String>): AppShellIntent {
        if (pathSegments.size < 2) return AppShellIntent.Home

        val uuidString = pathSegments[1]
        val userId = try {
            UserId(Uuid.parse(uuidString))
        } catch (_: IllegalArgumentException) {
            return AppShellIntent.Home
        }

        return AppShellIntent.ViewingProfile(userId)
    }
}