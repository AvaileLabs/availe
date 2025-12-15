package com.availelabs.orchestration.opensearch

import java.io.File
import java.security.MessageDigest

internal object OpenSearchSecurityHasher {

    fun calculateConfigurationHash(securityConfigurationDirectory: File): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")

        val filesToHash = listOf(
            "config.yml",
            "internal_users.yml",
            "roles.yml",
            "roles_mapping.yml",
            "tenants.yml",
            "audit.yml",
            "nodes_dn.yml",
            "whitelist.yml"
        ).sorted()

        filesToHash.forEach { fileName ->
            val file = File(securityConfigurationDirectory, fileName)
            if (file.exists()) {
                messageDigest.update(fileName.toByteArray())
                messageDigest.update(file.readBytes())
            }
        }

        val digestBytes = messageDigest.digest()
        return digestBytes.joinToString("") { byte -> "%02x".format(byte) }
    }

    fun calculateStaticAssetsHash(generatedConfigurationDirectory: File): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val certificatesDirectory = File(generatedConfigurationDirectory, "certs")

        // Hash the main configuration file
        val mainConfigurationFile = File(generatedConfigurationDirectory, "opensearch.yml")
        if (mainConfigurationFile.exists()) {
            messageDigest.update(mainConfigurationFile.name.toByteArray())
            messageDigest.update(mainConfigurationFile.readBytes())
        }

        // Hash all certificates
        if (certificatesDirectory.exists()) {
            certificatesDirectory.walk()
                .filter { it.isFile }
                .sortedBy { it.name }
                .forEach { file ->
                    messageDigest.update(file.name.toByteArray())
                    messageDigest.update(file.readBytes())
                }
        }

        val digestBytes = messageDigest.digest()
        return digestBytes.joinToString("") { byte -> "%02x".format(byte) }
    }
}