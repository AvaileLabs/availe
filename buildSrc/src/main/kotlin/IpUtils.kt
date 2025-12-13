package com.availelabs.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.net.Inet4Address
import java.net.NetworkInterface

abstract class HostIpValueSource : ValueSource<String, ValueSourceParameters.None> {

    override fun obtain(): String {
        return getHostIpAddress()
    }

    private fun getHostIpAddress(): String {
        val ipAddress = try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { it.inetAddresses.asSequence() }
                .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
                ?.hostAddress
        } catch (e: Exception) {
            throw GradleException("Could not determine host IP address. Error: ${e.message}")
        }
        return ipAddress
            ?: throw GradleException("Failed to find a suitable non-loopback IPv4 address for the host machine.")
    }
}