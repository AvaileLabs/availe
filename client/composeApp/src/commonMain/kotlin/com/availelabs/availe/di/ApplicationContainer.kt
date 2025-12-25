package com.availelabs.availe.di

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.availelabs.availe.core.platform.IInAppBrowserLauncher
import com.availelabs.availe.core.platform.createInAppBrowserLauncher
import com.availelabs.availe.platform.PlatformActivity
import com.availelabs.availe.repositories.IAuthenticationRepository
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.IMessagingRepository
import com.availelabs.availe.repositories.ISessionManager
import com.availelabs.availe.services.IMessageService

interface IApplicationContainer {
    val sessionManager: ISessionManager
    val authenticationRepository: IAuthenticationRepository
    val listingRepository: IListingRepository
    val messagingRepository: IMessagingRepository
    val messageService: IMessageService
    val inAppBrowserLauncher: IInAppBrowserLauncher
}

fun createApplicationContainer(
    lifecycle: Lifecycle,
    activity: PlatformActivity
): IApplicationContainer {
    val dataContainer = DataLayerFactory.create(activity, lifecycle)
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
    override val sessionManager: ISessionManager
        get() = dataContainer.sessionManager
    override val authenticationRepository: IAuthenticationRepository
        get() = dataContainer.authenticationRepository
    override val listingRepository: IListingRepository
        get() = dataContainer.listingRepository
    override val messagingRepository: IMessagingRepository
        get() = dataContainer.messagingRepository
    override val messageService: IMessageService
        get() = dataContainer.messageService
}