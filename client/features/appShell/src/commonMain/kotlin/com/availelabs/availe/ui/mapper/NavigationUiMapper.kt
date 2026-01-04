package com.availelabs.availe.ui.mapper

import availe.client.designsystem.generated.resources.*
import com.availelabs.availe.appshell.AppShellConfiguration
import com.availelabs.availe.ui.model.NavigationUiState
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle

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
                title = Res.string.navigation_details, // Or create a "New Listing" string resource
                icon = Lucide.FileText,
                contentDescription = Res.string.accessibility_navigation_details
            )
        }
    }
}