package com.availelabs.availe.repositories.user

import com.availelabs.availe.domain.model.*
import com.availelabs.availe.domain.types.DbFindResult
import com.example.jooq.tables.references.USERS
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.impl.DSL
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

interface IUserRepository {
    fun FindByJid(jid: Jid): DbFindResult<User>
    fun UpsertUser(jid: Jid, email: Email): User
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
    private val dsl: DSLContext
) : IUserRepository {

    override fun FindByJid(jid: Jid): DbFindResult<User> {
        val record = dsl.select(USERS.ID, USERS.JID, USERS.EMAIL)
            .from(USERS)
            .where(USERS.JID.eq(jid.value))
            .fetchOne()

        return if (record != null) {
            DbFindResult.Found(
                User(
                    id = UserId(record[USERS.ID]!!.toKotlinUuid()),
                    jid = Jid(record[USERS.JID]!!),
                    email = Email(record[USERS.EMAIL]!!)
                )
            )
        } else {
            DbFindResult.NotFound
        }
    }

    override fun UpsertUser(jid: Jid, email: Email): User {
        val record = dsl.insertInto(USERS)
            .set(USERS.ID, DSL.field("uuidv7()", UUID::class.java))
            .set(USERS.JID, jid.value)
            .set(USERS.EMAIL, email.value)
            // We set default values for required fields that aren't passed in provision
            .set(USERS.DISPLAY_NAME, email.value.substringBefore("@"))
            .returning(USERS.ID, USERS.JID, USERS.EMAIL)
            .fetchOne()
            ?: error("Failed to insert user")

        return User(
            id = UserId(record[USERS.ID]!!.toKotlinUuid()),
            jid = Jid(record[USERS.JID]!!),
            email = Email(record[USERS.EMAIL]!!)
        )
    }

    override fun GetUserById(id: UserId): PublicProfile? {
        return dsl.select(USERS.ID, USERS.DISPLAY_NAME, USERS.AVATAR_URL, USERS.JID)
            .from(USERS)
            .where(USERS.ID.eq(id.value.toJavaUuid()))
            .fetchOne()
            ?.let { record ->
                PublicProfile(
                    id = UserId(record[USERS.ID]!!.toKotlinUuid()),
                    displayName = UserDisplayName(record[USERS.DISPLAY_NAME]!!),
                    avatarUrl = record[USERS.AVATAR_URL]?.let { ImageUrl(it) },
                    jid = Jid(record[USERS.JID]!!)
                )
            }
    }

    override fun UpdateProfile(id: UserId, displayName: UserDisplayName?, avatarUrl: ImageUrl?) {
        val updates = mutableMapOf<Field<*>, Any?>()

        if (displayName != null) {
            updates[USERS.DISPLAY_NAME] = displayName.value
        }
        if (avatarUrl != null) {
            updates[USERS.AVATAR_URL] = avatarUrl.value
        }

        if (updates.isNotEmpty()) {
            dsl.update(USERS)
                .set(updates)
                .where(USERS.ID.eq(id.value.toJavaUuid()))
                .execute()
        }
    }
}