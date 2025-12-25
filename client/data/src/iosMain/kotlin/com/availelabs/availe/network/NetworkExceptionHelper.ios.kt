package com.availelabs.availe.network

import io.ktor.client.engine.darwin.*
import platform.Foundation.*

actual fun Throwable.classifyPlatformError(): NetworkErrorType? {
    if (this is DarwinHttpRequestException) {
        val underlyingError = this.origin
        if (underlyingError.domain == NSURLErrorDomain) {
            return when (underlyingError.code) {
                NSURLErrorNotConnectedToInternet -> NetworkErrorType.Recoverable.Connectivity
                NSURLErrorDataNotAllowed -> NetworkErrorType.Recoverable.Connectivity
                NSURLErrorInternationalRoamingOff -> NetworkErrorType.Recoverable.Connectivity
                NSURLErrorNetworkConnectionLost -> NetworkErrorType.Recoverable.Connectivity
                NSURLErrorCallIsActive -> NetworkErrorType.Recoverable.Connectivity

                NSURLErrorTimedOut -> NetworkErrorType.Recoverable.ConnectionTimeout

                NSURLErrorCannotFindHost -> NetworkErrorType.Recoverable.ServerSide
                NSURLErrorCannotConnectToHost -> NetworkErrorType.Recoverable.ServerSide

                NSURLErrorSecureConnectionFailed -> NetworkErrorType.Recoverable.ServerSide
                NSURLErrorServerCertificateHasBadDate -> NetworkErrorType.Recoverable.ServerSide
                NSURLErrorServerCertificateUntrusted -> NetworkErrorType.Recoverable.ServerSide
                NSURLErrorServerCertificateNotYetValid -> NetworkErrorType.Recoverable.ServerSide

                else -> NetworkErrorType.Recoverable.Connectivity
            }
        }
        return NetworkErrorType.Recoverable.Connectivity
    }
    return null
}