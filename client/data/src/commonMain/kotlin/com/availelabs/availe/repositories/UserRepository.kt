package com.availelabs.availe.repositories

import com.availelabs.availe.domain.model.PublicProfile
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.domain.types.FindResult
import com.availelabs.availe.model.error.AppError
import com.availelabs.availe.network.ISafeNetworkCaller
import com.availelabs.availe.sources.remote.IUserRemoteDataSource
import com.availelabs.availe.types.RepositoryOperationResult

internal class UserRepository(
    private val remoteDataSource: IUserRemoteDataSource,
    private val safeNetworkCaller: ISafeNetworkCaller
) : IUserRepository {

    override suspend fun getUserById(userId: UserId): RepositoryOperationResult<PublicProfile> {
        val result = safeNetworkCaller.execute {
            remoteDataSource.getUserById(userId)
        }

        return when (result) {
            is RepositoryOperationResult.Success -> {
                when (val findResult = result.value) {
                    is FindResult.Found -> RepositoryOperationResult.Success(findResult.value)
                    is FindResult.NotFound -> RepositoryOperationResult.Failure(AppError.UnknownError("User not found"))
                }
            }

            is RepositoryOperationResult.Failure -> result
        }
    }
}