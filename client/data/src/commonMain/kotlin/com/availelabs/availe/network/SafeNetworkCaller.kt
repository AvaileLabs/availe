package com.availelabs.availe.network

import com.availelabs.availe.sources.local.ILocalDataCleaner
import com.availelabs.availe.types.RepositoryOperationResult
import kotlinx.coroutines.CancellationException

internal interface ISafeNetworkCaller {
    suspend fun <T> execute(operation: suspend () -> T): RepositoryOperationResult<T>
}

internal class SafeNetworkCaller(
    private val localDataCleaner: ILocalDataCleaner
) : ISafeNetworkCaller {

    override suspend fun <T> execute(operation: suspend () -> T): RepositoryOperationResult<T> {
        return try {
            RepositoryOperationResult.Success(operation())
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            RepositoryOperationResult.Failure(throwable.toAppError())
        }
    }
}