package com.availelabs.availe.service

import com.availelabs.availe.domain.model.ImageUrl
import com.availelabs.availe.domain.model.UserDisplayName
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.service.ICacheService
import com.availelabs.availe.repositories.user.IUserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class UserProfileServiceTest {

    private val fakeCacheService = object : ICacheService {
        var removedKey: String? = null

        override fun <T : Any> Get(key: String, type: KClass<T>): T? = null
        override fun <T : Any> Set(key: String, value: T, timeToLiveSeconds: Int) {}

        override fun Remove(key: String) {
            removedKey = key
        }
    }

    private val fakeUserRepository = object : IUserRepository {
        var updateCalled = false

        override fun FindByJid(jid: com.availelabs.availe.domain.model.Jid) = throw NotImplementedError()
        override fun UpsertUser(
            jid: com.availelabs.availe.domain.model.Jid,
            email: com.availelabs.availe.domain.model.Email
        ) = throw NotImplementedError()

        override fun GetUserById(id: UserId) = throw NotImplementedError()

        override fun UpdateProfile(id: UserId, displayName: UserDisplayName?, avatarUrl: ImageUrl?) {
            updateCalled = true
        }
    }

    private val service = UserProfileService(fakeUserRepository, fakeCacheService)

    @Test
    fun `UpdateProfile calls repository and invalidates correct cache key`() {
        val userId = UserId(Uuid.random())
        val subject = "keycloak|12345"
        val expectedCacheKey = "user:$subject"

        service.UpdateProfile(
            id = userId,
            displayName = UserDisplayName("New Name"),
            avatarUrl = null,
            subject = subject
        )

        assertEquals(true, fakeUserRepository.updateCalled)
        assertEquals(expectedCacheKey, fakeCacheService.removedKey)
    }
}