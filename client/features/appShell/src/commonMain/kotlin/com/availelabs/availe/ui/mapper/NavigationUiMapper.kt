package com.availelabs.availe.ui.mapper

import availe.client.designsystem.generated.resources.*
import com.availelabs.availe.appshell.model.AppShellConfiguration
import com.availelabs.availe.ui.model.NavigationUiState
import com.composables.icons.lucide.*

object NavigationUiMapper {
    fun map(configuration: AppShellConfiguration): NavigationUiState {
        return when (configuration) {
            is AppShellConfiguration.Dashboard -> NavigationUiState(
                title = Res.string.navigation_home,
                icon = Lucide.House,
                contentDescription = Res.string.accessibility_navigation_home
            )

            is AppShellConfiguration.Chat,
            is AppShellConfiguration.ChatOverlay -> NavigationUiState(
                title = Res.string.navigation_chat,
                icon = Lucide.MessageCircle,
                contentDescription = Res.string.accessibility_navigation_chat
            )

            is AppShellConfiguration.ListingEditor -> NavigationUiState(
                title = Res.string.navigation_details,
                icon = Lucide.FileText,
                contentDescription = Res.string.accessibility_navigation_details
            )

            is AppShellConfiguration.ListingDetails -> NavigationUiState(
                title = Res.string.navigation_details,
                icon = Lucide.FileText,
                contentDescription = Res.string.accessibility_navigation_details
            )

            is AppShellConfiguration.Profile -> NavigationUiState(
                title = Res.string.navigation_profile,
                icon = Lucide.User,
                contentDescription = Res.string.accessibility_navigation_profile
            )

            AppShellConfiguration.Settings -> NavigationUiState(
                title = Res.string.navigation_settings,
                icon = Lucide.Settings,
                contentDescription = Res.string.accessibility_navigation_settings
            )
        }
    }
}