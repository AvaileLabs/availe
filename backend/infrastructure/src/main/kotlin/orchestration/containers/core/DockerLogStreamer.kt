package com.availelabs.orchestration.containers.core

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.nio.channels.AsynchronousCloseException
import java.nio.charset.StandardCharsets

internal class DockerLogStreamer(private val dockerClient: DockerClient) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val activeStreams = mutableMapOf<String, LogCallback>()

    fun startStreaming(containerIdentifier: String, containerName: String) {
        if (activeStreams.containsKey(containerIdentifier)) {
            return
        }

        val logCallback = LogCallback(containerName)
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
    }

    private class LogCallback(private val containerName: String) : ResultCallback.Adapter<Frame>(), Closeable {
        private val logger = LoggerFactory.getLogger("container.$containerName")
        @Volatile
        var isClosed = false

        private val ignoredLogPatterns = listOf(
            "DestinationMigrationCoordinator",
            "PluginService:onIndexModule",
            "HashRing",
            "AllocationService",
            "ClusterApplierService",
            "PPLFuncImpTable",
            "QueryInsightsService",
            "PerformanceAnalyzer",
            "Generated correlation index name",
            "No Old Correlation Indices to delete",
            "No Old IOC Findings Indices to delete",
            "No Old Alert Indices to delete",
            "AuditLogImpl",
            "ConfigHelper",
            "MetadataMappingService",
            "MetadataCreateIndexService",
            "RecoverySourceHandler",
            "Reflections took",
            "LogTypeService",
            "Setting query insights index template priority",
            "Master key is a required config",
            "StreamTransportService is not available",
            "Cannot create type checker for function",
            "Registered query converter",
            "loaded module",
            "loaded plugin",
            "PluginService:onIndexModule",
            "Cluster health status changed from",
            "Cancelling the migration process",
            "Using local opensearch cluster as metadata store",
            "ExtensionsManager initialized",
            "sun.misc.Unsafe",
            "SLF4J",
            "NodeNotConnectedException",
            "failed to join",
            "Config override setting",
            "ClusterManagerTaskThrottler",
            "MLModelAutoReDeployer",
            "Authentication finally failed"
        )

        override fun onNext(frame: Frame?) {
            if (frame != null) {
                val logLine = String(frame.payload, StandardCharsets.UTF_8).trim()
                if (logLine.isNotBlank() && !isNoisyLog(logLine)) {
                    logger.info(logLine)
                }
            }
        }

        override fun onError(throwable: Throwable?) {
            // Suppress AsynchronousCloseException which happens on normal stream close
            if (throwable !is AsynchronousCloseException) {
                super.onError(throwable)
            }
        }

        private fun isNoisyLog(line: String): Boolean {
            return ignoredLogPatterns.any { line.contains(it, ignoreCase = true) }
        }

        override fun close() {
            isClosed = true
            super.close()
        }
    }
}