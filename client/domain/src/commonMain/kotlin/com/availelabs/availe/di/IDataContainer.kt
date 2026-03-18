package com.availelabs.availe.di

import com.availelabs.availe.repositories.*
import com.availelabs.availe.services.IMessageService

interface IDataContainer {
    val sessionRepository: ISessionRepository
    val authenticationRepository: IAuthenticationRepository
    val listingRepository: IListingRepository
    val eventRepository: IEventRepository
    val messagingRepository: IMessagingRepository
    val userRepository: IUserRepository
    val interactionRepository: IInteractionRepository
    val messageService: IMessageService
}