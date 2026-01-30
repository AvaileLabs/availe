package com.availelabs.availe.domain.service

import com.availelabs.availe.auth.VerifiedKeycloakToken
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.types.DbFindResult
import com.availelabs.availe.repositories.user.IUserRepository
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi

interface IUserProvisioningService {
    fun GetOrProvisionUser(verifiedKeycloakToken: VerifiedKeycloakToken): UserId
}

@OptIn(ExperimentalUuidApi::class)
class UserProvisioningService(
    private val cacheService: ICacheService,
    private val userRepository: IUserRepository
) : IUserProvisioningService {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    private data class CachedUser(
        val id: UserId
    )

    override fun GetOrProvisionUser(verifiedKeycloakToken: VerifiedKeycloakToken): UserId {
        val subject = verifiedKeycloakToken.subject
        val cacheKey = "user:$subject"

        val cachedUser = cacheService.Get(cacheKey, CachedUser::class)
        if (cachedUser != null) {
            return cachedUser.id
        }

        val jid = verifiedKeycloakToken.jid ?: throw IllegalStateException("JID is missing in the verified token.")
        val email = verifiedKeycloakToken.email

        val existingUserResult = userRepository.FindByJid(jid)

        val userId = when (existingUserResult) {
            is DbFindResult.Found -> existingUserResult.value.id
            is DbFindResult.NotFound -> {
                try {
                    val newUser = userRepository.UpsertUser(jid, email)
                    newUser.id
                } catch (e: Exception) {
                    logger.warn("Race condition detected during user provisioning. Attempting recovery. Error: ${e.message}")

                    // Race Condition Handling:
                    // Another thread might have inserted the user between our FindByJid and UpsertUser calls.
                    // We attempt to fetch the user one more time.
                    val retryResult = userRepository.FindByJid(jid)
                    if (retryResult is DbFindResult.Found) {
                        retryResult.value.id
                    } else {
                        // If we still can't find it, the error was something else (e.g., DB connection).
                        throw e
                    }
                }
            }
        }

        val userToCache = CachedUser(userId)
        cacheService.Set(cacheKey, userToCache, timeToLiveSeconds = 3600)

        return userId
    }
}