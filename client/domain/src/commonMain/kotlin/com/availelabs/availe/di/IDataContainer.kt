package com.availelabs.availe.di

import com.availelabs.availe.repositories.*
import com.availelabs.availe.services.IMessageService

interface IDataContainer {
    val sessionRepository: ISessionRepository
    val authenticationRepository: IAuthenticationRepository
    val listingRepository: IListingRepository
    val messagingRepository: IMessagingRepository
    val userRepository: IUserRepository
    val messageService: IMessageService
}