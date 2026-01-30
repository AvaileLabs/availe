package com.availelabs.availe.repositories.user

import com.availelabs.availe.auth.VerifiedKeycloakToken
import com.availelabs.availe.domain.model.*
import com.example.jooq.tables.references.USERS
import org.jooq.DSLContext
import org.jooq.Field
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

interface IUserRepository {
    fun upsertUser(token: VerifiedKeycloakToken)
    fun getUserById(id: UserId): PublicProfile?
    fun updateProfile(id: UserId, displayName: UserDisplayName?, avatarUrl: ImageUrl?)
}

@OptIn(ExperimentalUuidApi::class)
class UserRepository(
    private val dsl: DSLContext
) : IUserRepository {

    override fun upsertUser(token: VerifiedKeycloakToken) {
        dsl.insertInto(USERS)
            .set(USERS.ID, token.userId.value.toJavaUuid())
            .set(USERS.EMAIL, token.email.value)
            .set(USERS.DISPLAY_NAME, token.displayName.value)
            .set(USERS.AVATAR_URL, token.avatarUrl?.value)
            .set(USERS.JID, token.jid?.value)
            .onConflict(USERS.ID)
            .doUpdate()
            .set(USERS.EMAIL, token.email.value)
            .set(USERS.JID, token.jid?.value)
            .execute()
    }

    override fun getUserById(id: UserId): PublicProfile? {
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

    override fun updateProfile(id: UserId, displayName: UserDisplayName?, avatarUrl: ImageUrl?) {
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