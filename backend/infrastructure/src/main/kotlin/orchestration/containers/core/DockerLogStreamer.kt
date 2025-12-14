package com.availelabs.orchestration.containers.core

import com.availelabs.orchestration.containers.domain.ContainerRunSpecification.LogStrategy
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.nio.channels.AsynchronousCloseException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal class DockerLogStreamer(private val dockerClient: DockerClient) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val activeStreams = ConcurrentHashMap<String, LogCallback>()
    private val logBuffers = ConcurrentHashMap<String, MutableList<String>>()

    private val errorSignatures = listOf(
        "[error]",
        "[critical]",
        "ERROR:",
        "FATAL:",
        "Exception",
        "panic:"
    )

    fun startStreaming(
        containerIdentifier: String,
        containerName: String,
        logStrategy: LogStrategy
    ) {
        if (logStrategy == LogStrategy.SILENT) {
            return
        }

        if (activeStreams.containsKey(containerIdentifier)) {
            return
        }

        val containerLogBuffer = Collections.synchronizedList(ArrayList<String>())
        logBuffers[containerIdentifier] = containerLogBuffer

        val logCallback = LogCallback(containerName, logStrategy, containerLogBuffer)
        activeStreams[containerIdentifier] = logCallback

        val logThread = Thread {
            try {
                dockerClient.logContainerCmd(containerIdentifier)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .withTailAll()
                    .exec(logCallback)

                logCallback.awaitCompletion()
            } catch (exception: Exception) {
                if (!logCallback.isClosed && exception !is AsynchronousCloseException) {
                    logger.warn("Log streaming failed for container '{}': {}", containerName, exception.message)
                }
            }
        }

        logThread.isDaemon = true
        logThread.name = "log-streamer-$containerName"
        logThread.start()
    }

    fun stopStreaming(containerIdentifier: String) {
        activeStreams.remove(containerIdentifier)?.close()
        logBuffers.remove(containerIdentifier)
    }

    fun dumpLogs(containerIdentifier: String, containerName: String) {
        val buffer = logBuffers[containerIdentifier]
        if (buffer != null) {
            val failureLogger = LoggerFactory.getLogger("container.$containerName")
            logger.error(">>> DUMPING ALL LOGS FOR FAILED CONTAINER: {} <<<", containerName.uppercase())

            synchronized(buffer) {
                buffer.forEach { logLine ->
                    failureLogger.error(logLine)
                }
            }

            logger.error(">>> END LOG DUMP FOR {} <<<", containerName.uppercase())
        } else {
            logger.warn("No buffered logs found for container '{}' (ID: {}).", containerName, containerIdentifier)
        }
    }

    fun containsErrorPatterns(containerIdentifier: String): Boolean {
        val buffer = logBuffers[containerIdentifier] ?: return false

        synchronized(buffer) {
            return buffer.any { logLine ->
                errorSignatures.any { signature -> logLine.contains(signature, ignoreCase = false) }
            }
        }
    }

    private class LogCallback(
        containerName: String,
        private val logStrategy: LogStrategy,
        private val logBuffer: MutableList<String>
    ) : ResultCallback.Adapter<Frame>(), Closeable {
        private val logger = LoggerFactory.getLogger("container.$containerName")

        @Volatile
        var isClosed = false

        override fun onNext(frame: Frame?) {
            if (frame != null) {
                val logLine = String(frame.payload, StandardCharsets.UTF_8).trim()
                if (logLine.isNotBlank()) {
                    logBuffer.add(logLine)

                    if (logStrategy == LogStrategy.STREAM_ALWAYS) {
                        logger.info(logLine)
                    }
                }
            }
        }

        override fun onError(throwable: Throwable?) {
            if (throwable !is AsynchronousCloseException) {
                super.onError(throwable)
            }
        }

        override fun close() {
            isClosed = true
            super.close()
        }
    }
}