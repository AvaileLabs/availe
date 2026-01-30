@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.repositories.user

import com.availelabs.availe.auth.VerifiedKeycloakToken
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.testutils.PostgresTestBase
import com.example.jooq.tables.references.USERS
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class UserRepositoryTest : PostgresTestBase() {

    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setupRepository() {
        dsl.deleteFrom(USERS).execute()
        userRepository = UserRepository(dsl)
    }

    private fun createVerifiedToken(
        id: UserId = UserId(Uuid.random()),
        email: Email = Email("test@example.com"),
        displayName: UserDisplayName = UserDisplayName("Test User"),
        avatarUrl: ImageUrl? = null,
        jid: Jid? = Jid("test@example.com")
    ): VerifiedKeycloakToken {
        return VerifiedKeycloakToken(
            userId = id,
            email = email,
            displayName = displayName,
            avatarUrl = avatarUrl,
            jid = jid
        )
    }

    @Test
    fun `upsertUser inserts a new user correctly`() {
        val token = createVerifiedToken(
            displayName = UserDisplayName("Alice"),
            avatarUrl = ImageUrl("http://example.com/alice.jpg"),
            jid = Jid("alice@xmpp")
        )

        userRepository.upsertUser(token)

        val retrievedUser = userRepository.getUserById(token.userId)
        assertNotNull(retrievedUser)
        assertEquals(token.userId, retrievedUser?.id)
        assertEquals(token.displayName, retrievedUser?.displayName)
        assertEquals(token.avatarUrl, retrievedUser?.avatarUrl)
        assertEquals(token.jid, retrievedUser?.jid)
    }

    @Test
    fun `upsertUser updates email and jid on conflict`() {
        val userId = UserId(Uuid.random())
        val originalToken = createVerifiedToken(
            id = userId,
            email = Email("old@example.com"),
            jid = Jid("old@xmpp")
        )
        userRepository.upsertUser(originalToken)

        val updatedToken = createVerifiedToken(
            id = userId,
            email = Email("new@example.com"),
            jid = Jid("new@xmpp"),
            displayName = UserDisplayName("Ignored Update")
        )
        userRepository.upsertUser(updatedToken)

        val retrievedUser = userRepository.getUserById(userId)
        assertNotNull(retrievedUser)
        assertEquals(Jid("new@xmpp"), retrievedUser?.jid)

        val record = dsl.select(USERS.EMAIL).from(USERS).where(USERS.ID.eq(userId.value.toJavaUuid())).fetchOne()
        assertEquals("new@example.com", record?.get(USERS.EMAIL))
    }

    @Test
    fun `updateProfile updates display name and avatar url`() {
        val token = createVerifiedToken(
            displayName = UserDisplayName("Original Name"),
            avatarUrl = ImageUrl("http://original.com")
        )
        userRepository.upsertUser(token)

        val newDisplayName = UserDisplayName("Updated Name")
        val newAvatarUrl = ImageUrl("http://updated.com")

        userRepository.updateProfile(token.userId, newDisplayName, newAvatarUrl)

        val retrievedUser = userRepository.getUserById(token.userId)
        assertNotNull(retrievedUser)
        assertEquals(newDisplayName, retrievedUser?.displayName)
        assertEquals(newAvatarUrl, retrievedUser?.avatarUrl)
    }

    @Test
    fun `updateProfile ignores null values`() {
        val originalDisplayName = UserDisplayName("Original Name")
        val originalAvatarUrl = ImageUrl("http://original.com")
        val token = createVerifiedToken(
            displayName = originalDisplayName,
            avatarUrl = originalAvatarUrl
        )
        userRepository.upsertUser(token)

        userRepository.updateProfile(token.userId, displayName = null, avatarUrl = null)

        val retrievedUser = userRepository.getUserById(token.userId)
        assertNotNull(retrievedUser)
        assertEquals(originalDisplayName, retrievedUser?.displayName)
        assertEquals(originalAvatarUrl, retrievedUser?.avatarUrl)
    }

    @Test
    fun `updateProfile updates only display name when avatar is null`() {
        val originalAvatarUrl = ImageUrl("http://original.com")
        val token = createVerifiedToken(
            displayName = UserDisplayName("Original Name"),
            avatarUrl = originalAvatarUrl
        )
        userRepository.upsertUser(token)

        val newDisplayName = UserDisplayName("New Name")
        userRepository.updateProfile(token.userId, displayName = newDisplayName, avatarUrl = null)

        val retrievedUser = userRepository.getUserById(token.userId)
        assertEquals(newDisplayName, retrievedUser?.displayName)
        assertEquals(originalAvatarUrl, retrievedUser?.avatarUrl)
    }

    @Test
    fun `getUserById returns null for non-existent user`() {
        val nonExistentId = UserId(Uuid.random())
        val result = userRepository.getUserById(nonExistentId)
        assertNull(result)
    }
}