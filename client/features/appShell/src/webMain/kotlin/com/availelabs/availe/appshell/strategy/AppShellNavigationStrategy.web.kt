package com.availelabs.availe.appshell.strategy

import com.availelabs.availe.appshell.component.AppShellChildComponent
import com.availelabs.availe.appshell.component.AppShellChildFactoryContext
import com.availelabs.availe.appshell.model.AppShellConfiguration
import com.availelabs.availe.appshell.model.AppShellIntent

actual object AppShellNavigationStrategy {
    actual fun calculateInitialStack(intent: AppShellIntent): List<AppShellConfiguration> {
        return when (intent) {
            AppShellIntent.Home -> listOf(AppShellConfiguration.Home)

            is AppShellIntent.ViewingListing -> listOf(
                AppShellConfiguration.Home,
                AppShellConfiguration.ListingDetails(intent.listingId)
            )

            is AppShellIntent.ViewingListingWithChat -> listOf(
                AppShellConfiguration.Home,
                AppShellConfiguration.ListingDetails(intent.listingId),
                AppShellConfiguration.Chat(intent.recipientJid)
            )

            is AppShellIntent.Chatting -> listOf(
                AppShellConfiguration.Home,
                AppShellConfiguration.Chat(intent.recipientJid)
            )

            is AppShellIntent.ListingEditor -> listOf(
                AppShellConfiguration.Home,
                AppShellConfiguration.ListingEditor(intent.listingId)
            )

            is AppShellIntent.ViewingProfile -> listOf(
                AppShellConfiguration.Home,
                AppShellConfiguration.Profile(intent.userId)
            )

            AppShellIntent.Settings -> listOf(
                AppShellConfiguration.Home,
                AppShellConfiguration.Settings
            )
        }
    }

    actual fun createChild(
        configuration: AppShellConfiguration,
        context: AppShellChildFactoryContext
    ): AppShellChildComponent {
        return when (configuration) {
            AppShellConfiguration.Home -> AppShellChildComponent.Home(
                context.homeFactory(context.componentContext)
            )

            is AppShellConfiguration.Chat -> AppShellChildComponent.Chat(
                context.chatRootFactory(context.componentContext, configuration.recipientJid)
            )

            is AppShellConfiguration.ListingEditor -> AppShellChildComponent.ListingEditor(
                com.availelabs.availe.listingeditor.ListingEditorComponent(
                    componentContext = context.componentContext,
                    listingId = configuration.listingId,
                    listingRepository = context.appContainer.listingRepository,
                    sessionManager = context.appContainer.sessionManager,
                    messageService = context.appContainer.messageService,
                    navigator = context.navigator
                )
            )

            is AppShellConfiguration.ListingDetails -> AppShellChildComponent.ListingDetails(
                context.listingDetailsFactory(context.componentContext, configuration.listingId)
            )

            is AppShellConfiguration.Profile -> AppShellChildComponent.Profile(
                context.profileFactory(context.componentContext, configuration.userId)
            )

            AppShellConfiguration.Settings -> AppShellChildComponent.Settings(
                context.settingsFactory(context.componentContext)
            )

            AppShellConfiguration.Dashboard -> {
                throw IllegalStateException("Dashboard configuration is not supported on Web")
            }

            is AppShellConfiguration.ChatOverlay -> {
                throw IllegalStateException("ChatOverlay should be handled by the SlotNavigation, not StackNavigation")
            }
        }
    }
}