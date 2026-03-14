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
        val authenticationSubject = verifiedKeycloakToken.subject
        val cacheKey = "user:$authenticationSubject"

        val cachedUser = cacheService.Get(cacheKey, CachedUser::class)
        if (cachedUser != null) {
            return cachedUser.id
        }

        val targetUserId = verifiedKeycloakToken.userId
        val userJabberIdentifier =
            verifiedKeycloakToken.jid ?: throw IllegalStateException("JID is missing in the verified token.")
        val userEmailAddress = verifiedKeycloakToken.email

        val resolvedUserId = when (val existingUserDatabaseResult = userRepository.FindById(targetUserId)) {
            is DbFindResult.Found -> existingUserDatabaseResult.value.id
            is DbFindResult.NotFound -> {
                try {
                    val provisionedUser = userRepository.UpsertUser(
                        id = targetUserId,
                        jid = userJabberIdentifier,
                        email = userEmailAddress
                    )
                    provisionedUser.id
                } catch (databaseException: Exception) {
                    logger.warn("Race condition detected during user provisioning. Attempting recovery. Error: ${databaseException.message}")

                    val retryDatabaseResult = userRepository.FindById(targetUserId)
                    if (retryDatabaseResult is DbFindResult.Found) {
                        retryDatabaseResult.value.id
                    } else {
                        throw databaseException
                    }
                }
            }
        }

        val userToCache = CachedUser(resolvedUserId)
        cacheService.Set(cacheKey, userToCache, timeToLiveSeconds = 3600)

        return resolvedUserId
    }
}