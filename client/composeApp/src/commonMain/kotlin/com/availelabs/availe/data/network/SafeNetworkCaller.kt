package com.availelabs.availe.data.network

import com.availelabs.availe.data.sources.local.ILocalDataCleaner
import com.availelabs.availe.domain.types.RepositoryOperationResult
import kotlinx.coroutines.CancellationException

interface ISafeNetworkCaller {
    suspend fun <T> execute(operation: suspend () -> T): RepositoryOperationResult<T>
}

class SafeNetworkCaller(
    private val localDataCleaner: ILocalDataCleaner
) : ISafeNetworkCaller {

    override suspend fun <T> execute(operation: suspend () -> T): RepositoryOperationResult<T> {
        return try {
            RepositoryOperationResult.Success(operation())
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            val networkErrorType = throwable.classifyNetworkError()

            if (!networkErrorType.isSafeToPreserveLocalData) {
                localDataCleaner.clearAllData()
            }

            RepositoryOperationResult.Failure(throwable.toAppError())
        }
    }
}