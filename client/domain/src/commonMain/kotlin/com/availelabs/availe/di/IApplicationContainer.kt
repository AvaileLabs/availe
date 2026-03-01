package com.availelabs.availe.di

import com.availelabs.availe.core.platform.IInAppBrowserLauncher
import com.availelabs.availe.repositories.*
import com.availelabs.availe.services.IMessageService

interface IApplicationContainer {
    val sessionManager: ISessionRepository
    val authenticationRepository: IAuthenticationRepository
    val listingRepository: IListingRepository
    val messagingRepository: IMessagingRepository
    val userRepository: IUserRepository
    val interactionRepository: IInteractionRepository
    val messageService: IMessageService
    val inAppBrowserLauncher: IInAppBrowserLauncher
}