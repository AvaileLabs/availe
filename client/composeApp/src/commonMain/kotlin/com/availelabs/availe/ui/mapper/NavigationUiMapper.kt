package com.availelabs.availe.ui.mapper

import availe.client.composeapp.generated.resources.*
import com.availelabs.availe.features.appshell.AppShellConfiguration
import com.availelabs.availe.ui.model.NavigationUiState
import com.composables.icons.lucide.*

object NavigationUiMapper {
    fun map(configuration: AppShellConfiguration): NavigationUiState {
        return when (configuration) {
            is AppShellConfiguration.Home -> NavigationUiState(
                title = Res.string.navigation_home,
                icon = Lucide.House,
                contentDescription = Res.string.accessibility_navigation_home
            )

            is AppShellConfiguration.ListingDetails -> NavigationUiState(
                title = Res.string.navigation_details,
                icon = Lucide.House,
                contentDescription = Res.string.accessibility_navigation_details
            )

            is AppShellConfiguration.Chat -> NavigationUiState(
                title = Res.string.navigation_chat,
                icon = Lucide.MessageCircle,
                contentDescription = Res.string.accessibility_navigation_chat
            )

            is AppShellConfiguration.Settings -> NavigationUiState(
                title = Res.string.navigation_settings,
                icon = Lucide.Settings,
                contentDescription = Res.string.accessibility_navigation_settings
            )

            is AppShellConfiguration.ListingEditor -> NavigationUiState(
                title = Res.string.navigation_details,
                icon = Lucide.House,
                contentDescription = Res.string.accessibility_navigation_details
            )

            is AppShellConfiguration.Profile -> NavigationUiState(
                title = Res.string.navigation_profile,
                icon = Lucide.User,
                contentDescription = Res.string.accessibility_navigation_profile
            )
        }
    }
}