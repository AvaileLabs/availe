package com.availelabs.availe.dto

import com.availelabs.availe.configuration.SharedAuthenticationConstants
import com.availelabs.availe.domain.model.Email
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class JwtPayloadDto(
    @SerialName(SharedAuthenticationConstants.Claims.SUBJECT) val subject: UserId,
    @SerialName(SharedAuthenticationConstants.Claims.EMAIL) val email: Email? = null,
    @SerialName(SharedAuthenticationConstants.Claims.JID) val jid: Jid? = null
)