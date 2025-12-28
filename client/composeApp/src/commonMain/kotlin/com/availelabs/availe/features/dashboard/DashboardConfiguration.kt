@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.dashboard

import com.availelabs.availe.domain.model.UserId
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@Serializable
sealed interface DashboardConfiguration {
    @Serializable
    data object Home : DashboardConfiguration

    @Serializable
    data class Profile(val userId: UserId) : DashboardConfiguration
}