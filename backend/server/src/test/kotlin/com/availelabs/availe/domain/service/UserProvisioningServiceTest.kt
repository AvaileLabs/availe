package com.availelabs.availe.domain.service

import com.availelabs.availe.auth.VerifiedKeycloakToken
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DbFindResult
import com.availelabs.availe.repositories.user.IUserRepository
import com.availelabs.availe.repositories.user.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class UserProvisioningServiceTest {

    private val fakeCacheService = object : ICacheService {
        var getResult: Any? = null
        var setCalledWith: Any? = null

        override fun <T : Any> Get(key: String, type: KClass<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return getResult as T?
        }

        override fun <T : Any> Set(key: String, value: T, timeToLiveSeconds: Int) {
            setCalledWith = value
        }

        override fun Remove(key: String) {}
    }

    private val fakeUserRepository = object : IUserRepository {
        var findResult: DbFindResult<User> = DbFindResult.NotFound
        var secondFindResult: DbFindResult<User>? = null // For race condition retry

        var upsertResult: User? = null
        var shouldThrowOnUpsert: Boolean = false
        var upsertCalled = AtomicBoolean(false)

        override fun FindByJid(jid: Jid): DbFindResult<User> {
            return if (secondFindResult != null && upsertCalled.get()) {
                val result = secondFindResult!!
                secondFindResult = null
                result
            } else {
                findResult
            }
        }

        override fun UpsertUser(jid: Jid, email: Email): User {
            upsertCalled.set(true)
            if (shouldThrowOnUpsert) {
                throw RuntimeException("Duplicate Key Violation")
            }
            return upsertResult ?: throw IllegalStateException("Mock upsert result not set")
        }

        override fun GetUserById(id: UserId): PublicProfile? = null
        override fun UpdateProfile(id: UserId, displayName: UserDisplayName?, avatarUrl: ImageUrl?) {}
    }

    private val service = UserProvisioningService(fakeCacheService, fakeUserRepository)

    private val testToken = VerifiedKeycloakToken(
        userId = UserId(Uuid.random()),
        email = Email("test@example.com"),
        displayName = UserDisplayName("Test"),
        avatarUrl = null,
        jid = Jid("test@xmpp"),
        subject = "keycloak_sub_123"
    )

    private fun createCachedUser(id: UserId): Any {
        val clazz = Class.forName("com.availelabs.availe.domain.service.UserProvisioningService\$CachedUser")
        val constructor = clazz.getDeclaredConstructor(Uuid::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(id.value)
    }

    @Test
    fun `GetOrProvisionUser returns cached ID on cache hit and skips DB`() {
        val cachedUserId = UserId(Uuid.random())
        fakeCacheService.getResult = createCachedUser(cachedUserId)

        val result = service.GetOrProvisionUser(testToken)

        assertEquals(cachedUserId, result)
        assertEquals(false, fakeUserRepository.upsertCalled.get())
    }

    @Test
    fun `GetOrProvisionUser queries DB, returns existing user, and populates cache on Cache Miss + DB Hit`() {
        fakeCacheService.getResult = null

        val existingDbUser = User(
            id = UserId(Uuid.random()),
            jid = testToken.jid!!,
            email = testToken.email
        )
        fakeUserRepository.findResult = DbFindResult.Found(existingDbUser)

        val result = service.GetOrProvisionUser(testToken)

        assertEquals(existingDbUser.id, result)
        assertEquals(false, fakeUserRepository.upsertCalled.get())

        val cachedUser = createCachedUser(existingDbUser.id)
        assertEquals(cachedUser, fakeCacheService.setCalledWith)
    }

    @Test
    fun `GetOrProvisionUser creates new user and populates cache on Cache Miss + DB Miss`() {
        fakeCacheService.getResult = null
        fakeUserRepository.findResult = DbFindResult.NotFound

        val newUser = User(
            id = UserId(Uuid.random()),
            jid = testToken.jid!!,
            email = testToken.email
        )
        fakeUserRepository.upsertResult = newUser

        val result = service.GetOrProvisionUser(testToken)

        assertEquals(newUser.id, result)
        assertEquals(true, fakeUserRepository.upsertCalled.get())

        val cachedUser = createCachedUser(newUser.id)
        assertEquals(cachedUser, fakeCacheService.setCalledWith)
    }

    @Test
    fun `GetOrProvisionUser recovers from Race Condition (Upsert Fails, Retry Find Succeeds)`() {
        fakeCacheService.getResult = null

        // 1. First Find returns NotFound (Simulating "Miss")
        fakeUserRepository.findResult = DbFindResult.NotFound

        // 2. Upsert throws Exception (Simulating Race Condition / Duplicate)
        fakeUserRepository.shouldThrowOnUpsert = true

        // 3. Second Find (Retry) returns Found (Simulating other thread finished)
        val raceWinnerUser = User(
            id = UserId(Uuid.random()),
            jid = testToken.jid!!,
            email = testToken.email
        )
        fakeUserRepository.secondFindResult = DbFindResult.Found(raceWinnerUser)

        val result = service.GetOrProvisionUser(testToken)

        assertEquals(raceWinnerUser.id, result)
        assertEquals(true, fakeUserRepository.upsertCalled.get())

        // Ensure we still cached the result from the retry
        val cachedUser = createCachedUser(raceWinnerUser.id)
        assertEquals(cachedUser, fakeCacheService.setCalledWith)
    }
}