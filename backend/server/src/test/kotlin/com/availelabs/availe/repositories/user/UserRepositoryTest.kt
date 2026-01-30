package com.availelabs.availe.repositories.user

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.testutils.PostgresTestBase
import com.example.jooq.tables.references.USERS
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class UserRepositoryTest : PostgresTestBase() {

    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setupRepository() {
        dsl.deleteFrom(USERS).execute()
        userRepository = UserRepository(dsl)
    }

    @Test
    fun `UpsertUser inserts a new user correctly`() {
        val jid = Jid("alice@xmpp")
        val email = Email("test@example.com")

        val user = userRepository.UpsertUser(jid, email)

        val retrievedUser = userRepository.GetUserById(user.id)
        assertNotNull(retrievedUser)
        assertEquals(user.id, retrievedUser?.id)
        assertEquals(jid, retrievedUser?.jid)
        assertEquals("test", retrievedUser?.displayName?.value)
    }

    @Test
    fun `UpdateProfile updates display name and avatar url`() {
        val jid = Jid("alice@xmpp")
        val email = Email("test@example.com")
        val user = userRepository.UpsertUser(jid, email)

        val newDisplayName = UserDisplayName("Updated Name")
        val newAvatarUrl = ImageUrl("http://updated.com")

        userRepository.UpdateProfile(user.id, newDisplayName, newAvatarUrl)

        val retrievedUser = userRepository.GetUserById(user.id)
        assertNotNull(retrievedUser)
        assertEquals(newDisplayName, retrievedUser?.displayName)
        assertEquals(newAvatarUrl, retrievedUser?.avatarUrl)
    }

    @Test
    fun `UpdateProfile ignores null values`() {
        val jid = Jid("alice@xmpp")
        val email = Email("test@example.com")
        val user = userRepository.UpsertUser(jid, email)

        // Upsert sets a default display name
        val originalDisplayName = UserDisplayName("test")

        userRepository.UpdateProfile(user.id, displayName = null, avatarUrl = null)

        val retrievedUser = userRepository.GetUserById(user.id)
        assertNotNull(retrievedUser)
        assertEquals(originalDisplayName, retrievedUser?.displayName)
        assertNull(retrievedUser?.avatarUrl)
    }

    @Test
    fun `UpdateProfile updates only display name when avatar is null`() {
        val jid = Jid("alice@xmpp")
        val email = Email("test@example.com")
        val user = userRepository.UpsertUser(jid, email)

        val newDisplayName = UserDisplayName("New Name")
        userRepository.UpdateProfile(user.id, displayName = newDisplayName, avatarUrl = null)

        val retrievedUser = userRepository.GetUserById(user.id)
        assertEquals(newDisplayName, retrievedUser?.displayName)
        assertNull(retrievedUser?.avatarUrl)
    }

    @Test
    fun `GetUserById returns null for non-existent user`() {
        val nonExistentId = UserId(Uuid.random())
        val result = userRepository.GetUserById(nonExistentId)
        assertNull(result)
    }
}