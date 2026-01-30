package com.availelabs.availe.controllers

import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.UserDisplayName
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.dto.kreplica.PublicProfileDtoSchema
import com.availelabs.availe.repositories.user.IUserRepository
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.*
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.lens.Path
import org.http4k.lens.uuid
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class UsersController(
    private val userRepository: IUserRepository
) {
    private val userIdPath =
        Path.uuid().map({ UserId(it.toKotlinUuid()) }, { it.value.toJavaUuid() }).of("userId")
    private val responseBodyLens = Body.auto<PublicProfileDtoSchema.V1.Data>().toLens()

    val routes = listOf(
        getUserById()
    )

    private fun getUserById(): ContractRoute {
        return "/users" / userIdPath meta {
            summary = "Get public user profile"
            returning(Status.OK, responseBodyLens to exampleProfile)
            returning(Status.NOT_FOUND)
        } bindContract Method.GET to { userId ->
            { _: Request ->
                val publicProfile = userRepository.GetUserById(userId)

                if (publicProfile != null) {
                    val dto = PublicProfileDtoSchema.V1.Data(
                        id = publicProfile.id,
                        displayName = publicProfile.displayName,
                        avatarUrl = publicProfile.avatarUrl,
                        jid = publicProfile.jid
                    )
                    Response(Status.OK).with(responseBodyLens of dto)
                } else {
                    Response(Status.NOT_FOUND)
                }
            }
        }
    }

    private val exampleProfile = PublicProfileDtoSchema.V1.Data(
        id = UserId(Uuid.parse("00000000-0000-0000-0000-000000000000")),
        displayName = UserDisplayName("Alice"),
        avatarUrl = null,
        jid = Jid("alice@localhost")
    )
}