package com.availelabs.availe.appshell.strategy

import com.availelabs.availe.appshell.component.AppShellChildComponent
import com.availelabs.availe.appshell.component.AppShellChildFactoryContext
import com.availelabs.availe.appshell.model.AppShellConfiguration
import com.availelabs.availe.appshell.model.AppShellIntent
import com.availelabs.availe.dashboard.DashboardComponent
import com.availelabs.availe.listingeditor.ListingEditorComponent

actual object AppShellNavigationStrategy {
    actual fun calculateInitialStack(intent: AppShellIntent): List<AppShellConfiguration> {
        return listOf(AppShellConfiguration.Dashboard)
    }

    actual fun createChild(
        configuration: AppShellConfiguration,
        context: AppShellChildFactoryContext
    ): AppShellChildComponent {
        return when (configuration) {
            AppShellConfiguration.Dashboard -> AppShellChildComponent.Dashboard(
                DashboardComponent(
                    componentContext = context.componentContext,
                    sessionManager = context.appContainer.sessionManager,
                    onShowLoginGuard = context.onShowLoginGuard,
                    initialIntent = context.initialIntent,
                    homeNavFactory = context.homeNavFactory,
                    chatRootFactory = context.chatRootFactory,
                    profileNavFactory = context.profileNavFactory
                )
            )

            is AppShellConfiguration.Home -> {
                throw IllegalStateException("Home is not a valid root in non-web platform")
            }

            is AppShellConfiguration.Chat -> AppShellChildComponent.Chat(
                context.chatRootFactory(context.componentContext, configuration.recipientJid)
            )

            is AppShellConfiguration.ListingEditor -> AppShellChildComponent.ListingEditor(
                ListingEditorComponent(
                    componentContext = context.componentContext,
                    listingId = configuration.listingId,
                    initialCategory = configuration.initialCategory,
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

            is AppShellConfiguration.ChatOverlay -> {
                throw IllegalStateException("ChatOverlay should be handled by the SlotNavigation, not StackNavigation")
            }
        }
    }
}