@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed interface UpsertResult {
    val id: Uuid

    data class Inserted(override val id: Uuid) : UpsertResult
    data class Found(override val id: Uuid) : UpsertResult
}