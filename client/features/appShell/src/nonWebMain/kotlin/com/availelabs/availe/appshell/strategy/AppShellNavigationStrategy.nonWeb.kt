package com.availelabs.availe.appshell.strategy

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.availelabs.availe.appshell.component.AppShellChildComponent
import com.availelabs.availe.appshell.component.AppShellChildFactoryContext
import com.availelabs.availe.appshell.model.AppShellConfiguration
import com.availelabs.availe.appshell.model.AppShellIntent
import com.availelabs.availe.dashboard.DashboardComponent
import com.availelabs.availe.listingdetails.ListingDetailsComponent
import com.availelabs.availe.listingeditor.ListingEditorComponent
import com.availelabs.availe.profile.ProfileComponent
import com.availelabs.availe.settings.SettingsComponent

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
                    listingRepository = context.appContainer.listingRepository,
                    sessionManager = context.appContainer.sessionManager,
                    messageService = context.appContainer.messageService,
                    navigator = context.navigator
                )
            )

            is AppShellConfiguration.ListingDetails -> AppShellChildComponent.ListingDetails(
                ListingDetailsComponent(
                    componentContext = context.componentContext,
                    listingId = configuration.listingId,
                    listingRepository = context.appContainer.listingRepository,
                    navigator = context.navigator
                )
            )

            is AppShellConfiguration.Profile -> AppShellChildComponent.Profile(
                ProfileComponent(
                    componentContext = context.componentContext,
                    targetUserId = configuration.userId,
                    listingRepository = context.appContainer.listingRepository,
                    sessionManager = context.appContainer.sessionManager,
                    messageService = context.appContainer.messageService,
                    navigator = context.navigator
                )
            )

            AppShellConfiguration.Settings -> AppShellChildComponent.Settings(
                SettingsComponent(
                    componentContext = context.componentContext,
                    storeFactory = DefaultStoreFactory(),
                    sessionManager = context.appContainer.sessionManager,
                    authenticationRepository = context.appContainer.authenticationRepository,
                    navigator = context.navigator
                )
            )

            is AppShellConfiguration.ChatOverlay -> {
                throw IllegalStateException("ChatOverlay should be handled by the SlotNavigation, not StackNavigation")
            }
        }
    }
}