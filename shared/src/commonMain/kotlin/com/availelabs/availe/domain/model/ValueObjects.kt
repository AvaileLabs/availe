package com.availelabs.availe.domain.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class ListingTitle(val value: String)

@Serializable
@JvmInline
value class ListingDescription(val value: String)

@Serializable
@JvmInline
value class PriceAmount(val value: String)

@Serializable
@JvmInline
value class ImageUrl(val value: String)