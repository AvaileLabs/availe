package com.availelabs.orchestration.hostsystem

import com.availelabs.utils.NativeAccess
import com.availelabs.utils.SudoExecutor
import org.slf4j.Logger
import java.io.File

internal class HostSystemManager(
    private val logger: Logger,
    private val hostSystemConfiguration: HostSystemConfig
) {

    private sealed interface OperatingSystem {
        data object Linux : OperatingSystem
        data object MacOs : OperatingSystem
        data object Unsupported : OperatingSystem
    }

    private fun detectOperatingSystem(): OperatingSystem {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("linux") -> OperatingSystem.Linux
            osName.contains("mac") -> OperatingSystem.MacOs
            else -> OperatingSystem.Unsupported
        }
    }

    fun setupHostSystem() {
        logger.info("Setting up host system configuration...")
        when (detectOperatingSystem()) {
            is OperatingSystem.Linux -> setupLinuxHost()
            is OperatingSystem.MacOs -> setupMacOsHost()
            is OperatingSystem.Unsupported -> logger.warn("Unsupported operating system detected. Skipping host tuning.")
        }
    }

    fun teardownHostSystem() {
        logger.info("Tearing down host system configuration...")
        when (detectOperatingSystem()) {
            is OperatingSystem.Linux -> teardownLinuxHost()
            is OperatingSystem.MacOs -> logger.info("No teardown actions required for macOS.")
            is OperatingSystem.Unsupported -> logger.warn("Unsupported operating system detected. Skipping host teardown.")
        }
    }

    private fun setupLinuxHost() {
        logger.info("Applying Linux host optimizations for OpenSearch...")

        SudoExecutor.executePrivilegedCommand(hostSystemConfiguration.sudoPassword, "swapoff -a")
        logger.info("Swap disabled (runtime).")

        SudoExecutor.executePrivilegedCommand(hostSystemConfiguration.sudoPassword, "sysctl -w vm.max_map_count=262144")
        logger.info("vm.max_map_count set to 262144 (runtime).")

        val temporaryConfigFile = File.createTempFile("opensearch_sysctl", ".conf")
        temporaryConfigFile.writeText("vm.max_map_count=262144\nvm.swappiness=1\n")

        val persistCommand = "cat ${temporaryConfigFile.absolutePath} | tee -a /etc/sysctl.conf"
        SudoExecutor.executePrivilegedCommand(hostSystemConfiguration.sudoPassword, persistCommand)
        logger.info("Configuration persisted to /etc/sysctl.conf.")

        SudoExecutor.executePrivilegedCommand(hostSystemConfiguration.sudoPassword, "sysctl -p")
        logger.info("Kernel settings reloaded from disk.")

        temporaryConfigFile.delete()
    }

    private fun teardownLinuxHost() {
        logger.info("Reverting Linux host optimizations...")

        SudoExecutor.executePrivilegedCommand(hostSystemConfiguration.sudoPassword, "swapon -a")
        logger.info("Swap re-enabled.")

        val sedCommand = "sed -i '/vm.max_map_count=262144/d; /vm.swappiness=1/d' /etc/sysctl.conf"
        SudoExecutor.executePrivilegedCommand(hostSystemConfiguration.sudoPassword, sedCommand)
        logger.info("Cleaned up /etc/sysctl.conf.")

        SudoExecutor.executePrivilegedCommand(hostSystemConfiguration.sudoPassword, "sysctl -p")
        logger.info("Kernel settings reloaded to apply cleanup.")
    }

    private fun setupMacOsHost() {
        logger.info("Validating macOS host settings...")
        try {
            val maxFilesPerProc = NativeAccess.getMacOsSysctlInt("kern.maxfilesperproc")
            if (maxFilesPerProc < 10240) {
                logger.warn("WARNING: kern.maxfilesperproc is $maxFilesPerProc. Docker Desktop may require at least 10240 for stability.")
            } else {
                logger.info("kern.maxfilesperproc is sufficient: $maxFilesPerProc")
            }
        } catch (exception: Exception) {
            logger.error("Failed to validate macOS settings: ${exception.message}")
        }
    }
}