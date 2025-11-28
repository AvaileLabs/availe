package com.availelabs.availe.domain.auth

import com.availelabs.availe.data.auth.JwtPayloadDto
import com.availelabs.availe.domain.model.Email
import com.availelabs.availe.domain.model.ImageUrl
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.model.UserName
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object JwtUserProfileMapper {
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

            val dto = json.decodeFromString<JwtPayloadDto>(decodedString)
            dto.toUserProfile()
        } catch (_: Exception) {
            null
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun JwtPayloadDto.toUserProfile(): UserProfile {
        val id = Uuid.parse(this.sub)
        val email = this.email ?: throw IllegalArgumentException("Email claim is required")
        val picture = this.picture
        val name = this.name

        return if (name != null) {
            UserProfile.Named(
                id = UserId(id),
                email = Email(email),
                pictureUrl = picture?.let { ImageUrl(it) },
                name = UserName(name)
            )
        } else {
            UserProfile.Anonymous(
                id = UserId(id),
                email = Email(email),
                pictureUrl = picture?.let { ImageUrl(it) }
            )
        }
    }
}