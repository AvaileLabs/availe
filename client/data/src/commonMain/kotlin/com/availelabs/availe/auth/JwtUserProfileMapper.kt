@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.auth

import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.dto.JwtPayloadDto
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.ExperimentalUuidApi

internal object JwtUserProfileMapper {
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalEncodingApi::class, ExperimentalUuidApi::class)
    fun decodeUserProfile(token: String): UserProfile? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null

            val payload = parts[1]
            val paddedPayload = when (payload.length % 4) {
                2 -> "$payload=="
                3 -> "$payload="
                else -> payload
            }

            val decodedBytes = Base64.UrlSafe.decode(paddedPayload)
            val decodedString = decodedBytes.decodeToString()

            val payloadDataTransferObject = json.decodeFromString<JwtPayloadDto>(decodedString)
            payloadDataTransferObject.toUserProfile()
        } catch (_: Exception) {
            null
        }
    }

    private fun JwtPayloadDto.toUserProfile(): UserProfile {
        val email = this.email ?: throw IllegalArgumentException("Email claim is required")
        val jidValue = this.jid?.value ?: throw IllegalArgumentException("JID claim is required")
        val userJid = Jid(jidValue)

        return UserProfile(
            id = this.subject,
            email = email,
            jid = userJid
        )
    }
}