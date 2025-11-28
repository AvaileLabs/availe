package com.availelabs.availe.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class JwtPayloadDto(
    val sub: String,
    val name: String? = null,
    val email: String? = null,
    val picture: String? = null
)