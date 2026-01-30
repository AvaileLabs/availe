package com.availelabs.availe.controllers

import com.availelabs.availe.auth.AuthenticationFilter
import com.availelabs.availe.auth.ITokenVerifier
import com.availelabs.availe.auth.VerifiedKeycloakToken
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.service.IUserProvisioningService
import com.availelabs.availe.dto.kreplica.PublicProfileDtoSchema
import com.availelabs.availe.repositories.user.IUserRepository
import com.availelabs.availe.service.IUserProfileService
import org.http4k.contract.contract
import org.http4k.core.*
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AccountControllerTest {

    private val validUserId = UserId(Uuid.random())
    private val validToken = "valid_token"
    private val validSubject = "keycloak_subject_123"

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

    private val fakeUserRepository = object : IUserRepository {
        val users = mutableMapOf<UserId, PublicProfile>()

        override fun FindByJid(jid: Jid) = throw NotImplementedError()
        override fun UpsertUser(jid: Jid, email: Email) = throw NotImplementedError()

        override fun GetUserById(id: UserId): PublicProfile? {
            return users[id]
        }

        override fun UpdateProfile(id: UserId, displayName: UserDisplayName?, avatarUrl: ImageUrl?) {
            val existing = users[id] ?: return
            users[id] = existing.copy(
                displayName = displayName ?: existing.displayName,
                avatarUrl = avatarUrl ?: existing.avatarUrl
            )
        }
    }

    private val fakeUserProvisioningService = object : IUserProvisioningService {
        override fun GetOrProvisionUser(verifiedKeycloakToken: VerifiedKeycloakToken): UserId {
            return verifiedKeycloakToken.userId
        }
    }

    private val fakeUserProfileService = object : IUserProfileService {
        override fun UpdateProfile(
            id: UserId,
            displayName: UserDisplayName?,
            avatarUrl: ImageUrl?,
            subject: String
        ) {
            fakeUserRepository.UpdateProfile(id, displayName, avatarUrl)
        }
    }

    private val authenticationFilter = AuthenticationFilter(
        tokenVerifier = fakeTokenVerifier,
        userProvisioningService = fakeUserProvisioningService,
        events = { _ -> }
    )

    private val controller = AccountController(fakeUserRepository, fakeUserProfileService, authenticationFilter)

    private val app = routes(
        "/api" bind contract {
            routes += controller.routes
        }
    )

    private val responseBodyLens = Body.auto<PublicProfileDtoSchema.V1.Data>().toLens()
    private val requestBodyLens = Body.auto<PublicProfileDtoSchema.V1.Data>().toLens()

    @Test
    fun `GET account returns user profile when authenticated`() {
        val request = Request(Method.GET, "/api/account")
            .header("Authorization", "Bearer $validToken")

        fakeUserRepository.users[validUserId] = PublicProfile(
            id = validUserId,
            displayName = UserDisplayName("Test"),
            avatarUrl = null,
            jid = Jid("jid")
        )

        val response = app(request)

        assertEquals(Status.OK, response.status)
        val profile = responseBodyLens(response)
        assertEquals(validUserId, profile.id)
    }

    @Test
    fun `PATCH account updates profile when authenticated`() {
        fakeUserRepository.users[validUserId] = PublicProfile(
            id = validUserId,
            displayName = UserDisplayName("Old Name"),
            avatarUrl = null,
            jid = Jid("jid")
        )

        val updateRequest = PublicProfileDtoSchema.V1.Data(
            id = validUserId,
            displayName = UserDisplayName("New Name"),
            avatarUrl = ImageUrl("http://new.com"),
            jid = Jid("jid")
        )

        val request = Request(Method.PATCH, "/api/account")
            .header("Authorization", "Bearer $validToken")
            .with(requestBodyLens of updateRequest)

        val response = app(request)

        assertEquals(Status.OK, response.status)

        val updatedUser = fakeUserRepository.GetUserById(validUserId)
        assertEquals(UserDisplayName("New Name"), updatedUser?.displayName)
        assertEquals(ImageUrl("http://new.com"), updatedUser?.avatarUrl)
    }

    @Test
    fun `GET account returns 401 when unauthorized`() {
        val request = Request(Method.GET, "/api/account")
            .header("Authorization", "Bearer invalid")

        val response = app(request)

        assertEquals(Status.UNAUTHORIZED, response.status)
    }
}