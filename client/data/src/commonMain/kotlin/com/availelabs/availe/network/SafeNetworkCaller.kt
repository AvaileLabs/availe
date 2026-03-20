package com.availelabs.availe.network

import com.availelabs.availe.logging.LogCategory
import com.availelabs.availe.logging.logger
import com.availelabs.availe.sources.local.ILocalDataCleaner
import com.availelabs.availe.types.RepositoryOperationResult
import kotlinx.coroutines.CancellationException

internal interface ISafeNetworkCaller {
    suspend fun <T> execute(operation: suspend () -> T): RepositoryOperationResult<T>
}

internal class SafeNetworkCaller(
    private val localDataCleaner: ILocalDataCleaner
) : ISafeNetworkCaller {

    private val classLogger by logger(LogCategory.NETWORK)

    override suspend fun <T> execute(operation: suspend () -> T): RepositoryOperationResult<T> {
        return try {
            RepositoryOperationResult.Success(operation())
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            classLogger.e(throwable) { "SafeNetworkCaller caught exception:" }
            RepositoryOperationResult.Failure(throwable.toAppError())
        }
    }
}