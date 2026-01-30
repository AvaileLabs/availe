package com.availelabs.availe.service

import com.availelabs.availe.domain.model.ImageUrl
import com.availelabs.availe.domain.model.UserDisplayName
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.service.ICacheService
import com.availelabs.availe.repositories.user.IUserRepository

interface IUserProfileService {
    fun UpdateProfile(id: UserId, displayName: UserDisplayName?, avatarUrl: ImageUrl?, subject: String)
}

class UserProfileService(
    private val userRepository: IUserRepository,
    private val cacheService: ICacheService
) : IUserProfileService {

    override fun UpdateProfile(id: UserId, displayName: UserDisplayName?, avatarUrl: ImageUrl?, subject: String) {
        userRepository.UpdateProfile(id, displayName, avatarUrl)
        cacheService.Remove("user:$subject")
    }
}