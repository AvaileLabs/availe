package com.availelabs.availe.repositories

import com.availelabs.availe.domain.model.PublicProfile
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.types.RepositoryOperationResult

interface IUserRepository {
    suspend fun getUserById(userId: UserId): RepositoryOperationResult<PublicProfile>
}