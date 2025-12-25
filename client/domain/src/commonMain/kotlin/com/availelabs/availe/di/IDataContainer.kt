package com.availelabs.availe.di

import com.availelabs.availe.repositories.IAuthenticationRepository
import com.availelabs.availe.repositories.IListingRepository
import com.availelabs.availe.repositories.IMessagingRepository
import com.availelabs.availe.repositories.ISessionRepository
import com.availelabs.availe.services.IMessageService

interface IDataContainer {
    val sessionRepository: ISessionRepository
    val authenticationRepository: IAuthenticationRepository
    val listingRepository: IListingRepository
    val messagingRepository: IMessagingRepository
    val messageService: IMessageService
}