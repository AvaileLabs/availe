package com.availelabs.availe.di

import com.availelabs.availe.core.platform.IInAppBrowserLauncher
import com.availelabs.availe.repositories.IAuthenticationRepository
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.IMessagingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.services.IMessageService

interface IApplicationContainer {
    val sessionManager: ISessionRepository
    val authenticationRepository: IAuthenticationRepository
    val listingRepository: IListingRepository
    val messagingRepository: IMessagingRepository
    val messageService: IMessageService
    val inAppBrowserLauncher: IInAppBrowserLauncher
}