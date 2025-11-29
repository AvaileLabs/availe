package com.availelabs.availe.data.dto

import com.availelabs.availe.domain.model.Email
import com.availelabs.availe.domain.model.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JwtPayloadDto(
    @SerialName("sub") val subject: UserId,
    @SerialName("email") val email: Email? = null
)