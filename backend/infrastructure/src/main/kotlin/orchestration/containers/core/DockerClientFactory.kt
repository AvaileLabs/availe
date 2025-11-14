package com.availelabs.orchestration.containers.core

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import java.nio.file.Files
import java.nio.file.Paths

internal class DockerClientProvider {

    private sealed class OperatingSystem {
        object LinuxOperatingSystem : OperatingSystem()
        object MacOperatingSystem : OperatingSystem()

        companion object {
            fun detect(): OperatingSystem = when (System.getProperty("os.name").lowercase()) {
                "mac os x", "darwin" -> MacOperatingSystem
                "linux" -> LinuxOperatingSystem
                else -> throw IllegalStateException("Unsupported operating system")
            }
        }
    }

    fun createDockerClient(): DockerClient {
        val dockerHostUniformResourceIdentifier = OperatingSystem.detect().let { operatingSystem ->
            System.getenv("DOCKER_HOST")?.takeIf { it.isNotBlank() } ?: when (operatingSystem) {
                is OperatingSystem.MacOperatingSystem -> {
                    val userHomeDirectory = System.getProperty("user.home")
                    val podmanSocketPath = Paths.get(userHomeDirectory, ".local/share/podman/podman.sock")
                    if (Files.exists(podmanSocketPath)) {
                        "unix://$podmanSocketPath"
                    } else {
                        "unix:///var/run/docker.sock"
                    }
                }

                is OperatingSystem.LinuxOperatingSystem -> {
                    val xdgRuntimeDirectory = System.getenv("XDG_RUNTIME_DIR")
                        ?: error("XDG_RUNTIME_DIR is not set. Cannot find rootless Podman socket.")
                    val rootlessSocketPath = Paths.get(xdgRuntimeDirectory, "podman/podman.sock")

                    if (Files.exists(rootlessSocketPath)) {
                        "unix://$rootlessSocketPath"
                    } else {
                        error("Rootless Podman socket not found at $rootlessSocketPath. Is Podman running?")
                    }
                }
            }
        }

        val dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(dockerHostUniformResourceIdentifier)
            .build()

        val apacheDockerHttpClient = ApacheDockerHttpClient.Builder()
            .dockerHost(dockerClientConfig.dockerHost)
            .sslConfig(dockerClientConfig.sslConfig)
            .build()

        return DockerClientImpl.getInstance(dockerClientConfig, apacheDockerHttpClient)
    }
}