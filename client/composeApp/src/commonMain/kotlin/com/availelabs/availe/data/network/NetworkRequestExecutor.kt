package com.availelabs.availe.data.network

import com.availelabs.availe.domain.model.UserFriendlyErrorMessage
import com.availelabs.availe.domain.types.RepositoryOperationResult
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.util.network.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

suspend fun <T> executeSafeNetworkRequest(
    operation: suspend () -> T
): RepositoryOperationResult<T> {
    return try {
        RepositoryOperationResult.Success(operation())
    } catch (_: UnresolvedAddressException) {
        RepositoryOperationResult.Failure(UserFriendlyErrorMessage("No internet connection. Please check your network."))
    } catch (_: HttpRequestTimeoutException) {
        RepositoryOperationResult.Failure(UserFriendlyErrorMessage("The request timed out. Please try again."))
    } catch (_: ConnectTimeoutException) {
        RepositoryOperationResult.Failure(UserFriendlyErrorMessage("The server is taking too long to respond."))
    } catch (_: SocketTimeoutException) {
        RepositoryOperationResult.Failure(UserFriendlyErrorMessage("The connection was interrupted. Please try again."))
    } catch (_: ClientRequestException) {
        RepositoryOperationResult.Failure(UserFriendlyErrorMessage("Unable to load this data. It may have been removed."))
    } catch (_: ServerResponseException) {
        RepositoryOperationResult.Failure(UserFriendlyErrorMessage("The server is experiencing issues. Please try again later."))
    } catch (_: SerializationException) {
        RepositoryOperationResult.Failure(UserFriendlyErrorMessage("Data error. Please ensure you have the latest version of the app."))
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        RepositoryOperationResult.Failure(UserFriendlyErrorMessage("Unable to connect to the server."))
    }
}