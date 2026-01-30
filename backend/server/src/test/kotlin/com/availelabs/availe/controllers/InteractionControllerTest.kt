package com.availelabs.availe.controllers

import com.availelabs.availe.auth.AuthenticationFilter
import com.availelabs.availe.auth.ITokenVerifier
import com.availelabs.availe.auth.VerifiedKeycloakToken
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.service.IUserProvisioningService
import com.availelabs.availe.dto.kreplica.InteractionRequestDtoSchema
import com.availelabs.availe.repositories.interaction.IInteractionRepository
import org.http4k.contract.contract
import org.http4k.core.*
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InteractionControllerTest {

    private val validUserId = UserId(Uuid.random())
    private val validToken = "valid_token"
    private val validSubject = "keycloak_subject_456"

    private val fakeTokenVerifier = object : ITokenVerifier {
        override fun verify(token: String): VerifiedKeycloakToken? {
            return if (token == validToken) {
                VerifiedKeycloakToken(
                    userId = validUserId,
                    email = Email("test@test.com"),
                    displayName = UserDisplayName("Test"),
                    avatarUrl = null,
                    jid = null,
                    subject = validSubject
                )
            } else {
                null
            }
        }
    }

    private val fakeUserProvisioningService = object : IUserProvisioningService {
        override fun GetOrProvisionUser(verifiedKeycloakToken: VerifiedKeycloakToken): UserId {
            return verifiedKeycloakToken.userId
        }
    }

    private val fakeInteractionRepository = object : IInteractionRepository {
        val interactions = mutableListOf<Triple<UserId, ListingId, InteractionType>>()

        override fun addInteraction(userId: UserId, listingId: ListingId, type: InteractionType) {
            interactions.add(Triple(userId, listingId, type))
        }

        override fun removeInteraction(userId: UserId, listingId: ListingId, type: InteractionType) {
            interactions.removeIf { it.first == userId && it.second == listingId && it.third == type }
        }
    }

    private val authenticationFilter = AuthenticationFilter(
        tokenVerifier = fakeTokenVerifier,
        userProvisioningService = fakeUserProvisioningService,
        events = { _ -> }
    )

    private val controller = InteractionController(fakeInteractionRepository, authenticationFilter)

    private val app = routes(
        "/api" bind contract {
            routes += controller.routes
        }
    )

    private val requestBodyLens = Body.auto<InteractionRequestDtoSchema.V1.Data>().toLens()

    @Test
    fun `PATCH interaction adds interaction when authenticated`() {
        val listingId = ListingId(Uuid.random())
        val body = InteractionRequestDtoSchema.V1.Data(
            action = InteractionAction.Add,
            type = InteractionType.Favorite
        )

        val request = Request(Method.PATCH, "/api/listings/${listingId.value}/interaction")
            .header("Authorization", "Bearer $validToken")
            .with(requestBodyLens of body)

        val response = app(request)

        assertEquals(Status.NO_CONTENT, response.status)
        assertTrue(fakeInteractionRepository.interactions.any {
            it.first == validUserId && it.second == listingId && it.third == InteractionType.Favorite
        })
    }

    @Test
    fun `PATCH interaction removes interaction when authenticated`() {
        val listingId = ListingId(Uuid.random())
        fakeInteractionRepository.addInteraction(validUserId, listingId, InteractionType.Favorite)

        val body = InteractionRequestDtoSchema.V1.Data(
            action = InteractionAction.Remove,
            type = InteractionType.Favorite
        )

        val request = Request(Method.PATCH, "/api/listings/${listingId.value}/interaction")
            .header("Authorization", "Bearer $validToken")
            .with(requestBodyLens of body)

        val response = app(request)

        assertEquals(Status.NO_CONTENT, response.status)
        assertTrue(fakeInteractionRepository.interactions.isEmpty())
    }

    @Test
    fun `PATCH interaction returns 401 when unauthorized`() {
        val listingId = ListingId(Uuid.random())
        val body = InteractionRequestDtoSchema.V1.Data(
            action = InteractionAction.Add,
            type = InteractionType.Favorite
        )

        val request = Request(Method.PATCH, "/api/listings/${listingId.value}/interaction")
            .header("Authorization", "Bearer invalid_token")
            .with(requestBodyLens of body)

        val response = app(request)

        assertEquals(Status.UNAUTHORIZED, response.status)
        assertTrue(fakeInteractionRepository.interactions.isEmpty())
    }
}