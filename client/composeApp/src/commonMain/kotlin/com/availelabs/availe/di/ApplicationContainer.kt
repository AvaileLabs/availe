package com.availelabs.availe.di

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.availelabs.availe.core.platform.IInAppBrowserLauncher
import com.availelabs.availe.core.platform.createInAppBrowserLauncher
import com.availelabs.availe.platform.PlatformActivity
import com.availelabs.availe.repositories.*
import com.availelabs.availe.services.IMessageService

fun createApplicationContainer(
    instanceKeeper: InstanceKeeper,
    activity: PlatformActivity
): IApplicationContainer {
    val dataContainer = DataLayerFactory.create(activity, instanceKeeper)
    val inAppBrowserLauncher = createInAppBrowserLauncher(activity)

    return ApplicationContainer(
        dataContainer = dataContainer,
        inAppBrowserLauncher = inAppBrowserLauncher
    )
}

private class ApplicationContainer(
    private val dataContainer: IDataContainer,
    override val inAppBrowserLauncher: IInAppBrowserLauncher
) : IApplicationContainer {
    override val sessionManager: ISessionRepository
        get() = dataContainer.sessionRepository
    override val authenticationRepository: IAuthenticationRepository
        get() = dataContainer.authenticationRepository
    override val listingRepository: IListingRepository
        get() = dataContainer.listingRepository
    override val messagingRepository: IMessagingRepository
        get() = dataContainer.messagingRepository
    override val userRepository: IUserRepository
        get() = dataContainer.userRepository
    override val interactionRepository: IInteractionRepository
        get() = dataContainer.interactionRepository
    override val messageService: IMessageService
        get() = dataContainer.messageService
}