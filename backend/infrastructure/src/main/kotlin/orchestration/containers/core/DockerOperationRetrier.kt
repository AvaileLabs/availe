package com.availelabs.orchestration.containers.core

import org.apache.hc.core5.http.NoHttpResponseException
import org.slf4j.LoggerFactory
import java.net.SocketException
import java.nio.channels.AsynchronousCloseException

internal object DockerOperationRetrier {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun <T> executeWithRetry(operationName: String, operation: () -> T): T {
        var attemptCounter = 0
        val maximumAttempts = 5
        var lastRecordedException: Exception? = null

        while (attemptCounter < maximumAttempts) {
            try {
                attemptCounter++
                return operation()
            } catch (exception: Exception) {
                if (isTransportError(exception)) {
                    val backoffMilliseconds = (1000L * Math.pow(2.0, (attemptCounter - 1).toDouble())).toLong()
                    logger.warn(
                        "Transport error during '{}' (Attempt {}/{}). Retrying in {}ms... Error: {}",
                        operationName,
                        attemptCounter,
                        maximumAttempts,
                        backoffMilliseconds,
                        exception.message
                    )
                    lastRecordedException = exception
                    Thread.sleep(backoffMilliseconds)
                } else {
                    throw exception
                }
            }
        }
        throw IllegalStateException(
            "Failed to complete operation '$operationName' after $maximumAttempts attempts.",
            lastRecordedException
        )
    }

    private fun isTransportError(exception: Throwable): Boolean {
        var currentException: Throwable? = exception
        while (currentException != null) {
            if (currentException is NoHttpResponseException ||
                currentException is SocketException ||
                currentException is AsynchronousCloseException ||
                currentException.message?.contains("failed to respond", ignoreCase = true) == true ||
                currentException.message?.contains("Connection reset", ignoreCase = true) == true
            ) {
                return true
            }
            currentException = currentException.cause
        }
        return false
    }
}