package com.availelabs.availe.sources.remote

import com.availelabs.availe.domain.model.PublicProfile
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.dto.kreplica.PublicProfileDtoSchema
import com.availelabs.availe.extensions.ensureSuccess
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi

internal interface IUserRemoteDataSource {
    suspend fun getUserById(userId: UserId): FindResult<PublicProfile>
}

internal class UserRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json
) : IUserRemoteDataSource {

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getUserById(userId: UserId): FindResult<PublicProfile> {
        val response = httpClient.get("/api/users/${userId.value}")

        return when (response.status) {
            HttpStatusCode.OK -> {
                val jsonString = response.bodyAsText()
                val dto = json.decodeFromString<PublicProfileDtoSchema.V1.Data>(jsonString)
                FindResult.Found(dto.toDomain())
            }

            HttpStatusCode.NotFound -> FindResult.NotFound
            else -> {
                response.ensureSuccess()
                throw IllegalStateException("Unexpected status code: ${response.status}")
            }
        }
    }

    private fun PublicProfileDtoSchema.V1.Data.toDomain(): PublicProfile {
        return PublicProfile(
            id = this.id,
            displayName = this.displayName,
            avatarUrl = this.avatarUrl,
            jid = this.jid
        )
    }
}