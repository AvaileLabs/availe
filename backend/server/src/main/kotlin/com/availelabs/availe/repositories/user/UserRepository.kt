package com.availelabs.availe.repositories.user

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DbFindResult
import com.example.jooq.tables.references.USERS
import org.jooq.DSLContext
import org.jooq.Field
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

interface IUserRepository {
    fun FindById(id: UserId): DbFindResult<User>
    fun UpsertUser(id: UserId, jid: Jid, email: Email): User
    fun GetUserById(id: UserId): PublicProfile?
    fun UpdateProfile(id: UserId, displayName: UserDisplayName?, avatarUrl: ImageUrl?)
}

data class User(
    val id: UserId,
    val jid: Jid,
    val email: Email
)

@OptIn(ExperimentalUuidApi::class)
class UserRepository(
    private val databaseContext: DSLContext
) : IUserRepository {

    override fun FindById(id: UserId): DbFindResult<User> {
        val databaseRecord = databaseContext.select(USERS.ID, USERS.JID, USERS.EMAIL)
            .from(USERS)
            .where(USERS.ID.eq(id.value.toJavaUuid()))
            .fetchOne()

        return if (databaseRecord != null) {
            DbFindResult.Found(
                User(
                    id = UserId(databaseRecord[USERS.ID]!!.toKotlinUuid()),
                    jid = Jid(databaseRecord[USERS.JID]!!),
                    email = Email(databaseRecord[USERS.EMAIL]!!)
                )
            )
        } else {
            DbFindResult.NotFound
        }
    }

    override fun UpsertUser(id: UserId, jid: Jid, email: Email): User {
        val databaseRecord = databaseContext.insertInto(USERS)
            .set(USERS.ID, id.value.toJavaUuid())
            .set(USERS.JID, jid.value)
            .set(USERS.EMAIL, email.value)
            .set(USERS.DISPLAY_NAME, email.value.substringBefore("@"))
            .returning(USERS.ID, USERS.JID, USERS.EMAIL)
            .fetchOne()
            ?: error("Failed to insert user")

        return User(
            id = UserId(databaseRecord[USERS.ID]!!.toKotlinUuid()),
            jid = Jid(databaseRecord[USERS.JID]!!),
            email = Email(databaseRecord[USERS.EMAIL]!!)
        )
    }

    override fun GetUserById(id: UserId): PublicProfile? {
        return databaseContext.select(USERS.ID, USERS.DISPLAY_NAME, USERS.AVATAR_URL, USERS.JID)
            .from(USERS)
            .where(USERS.ID.eq(id.value.toJavaUuid()))
            .fetchOne()
            ?.let { databaseRecord ->
                PublicProfile(
                    id = UserId(databaseRecord[USERS.ID]!!.toKotlinUuid()),
                    displayName = UserDisplayName(databaseRecord[USERS.DISPLAY_NAME]!!),
                    avatarUrl = databaseRecord[USERS.AVATAR_URL]?.let { avatarUrlString -> ImageUrl(avatarUrlString) },
                    jid = Jid(databaseRecord[USERS.JID]!!)
                )
            }
    }

    override fun UpdateProfile(id: UserId, displayName: UserDisplayName?, avatarUrl: ImageUrl?) {
        val profileUpdates = mutableMapOf<Field<*>, Any?>()

        if (displayName != null) {
            profileUpdates[USERS.DISPLAY_NAME] = displayName.value
        }
        if (avatarUrl != null) {
            profileUpdates[USERS.AVATAR_URL] = avatarUrl.value
        }

        if (profileUpdates.isNotEmpty()) {
            databaseContext.update(USERS)
                .set(profileUpdates)
                .where(USERS.ID.eq(id.value.toJavaUuid()))
                .execute()
        }
    }
}