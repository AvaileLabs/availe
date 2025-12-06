package com.availelabs.orchestration.ejabberd

import com.availelabs.availe.configuration.SharedAuthenticationConstants
import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.orchestration.keycloak.KeycloakPortsList
import com.availelabs.orchestration.postgres.PostgresConfig
import com.availelabs.orchestration.postgres.loadPostgresConfig
import com.availelabs.utils.loadGradleProperties
import java.io.File

internal data class EjabberdConfig(
    val adminUser: String,
    val adminPass: String
)

internal fun loadEjabberdConfig(): EjabberdConfig {
    val gradleProperties = loadGradleProperties()
    return EjabberdConfig(
        adminUser = gradleProperties.getProperty("ejabberd.admin.user")
            ?: error("No ejabberd.admin.user found in gradle.properties"),
        adminPass = gradleProperties.getProperty("ejabberd.admin.password")
            ?: error("No ejabberd.admin.password found in gradle.properties")
    )
}

internal fun ejabberdRunSpec(): ContainerRunSpecification<EjabberdPortsList, EjabberdVolumesList> {
    val ejabberdConfiguration = loadEjabberdConfig()
    val postgresConfiguration = loadPostgresConfig()

    val configurationTemporaryFile = createEjabberdConfigFileOnHost(ejabberdConfiguration, postgresConfiguration)

    return ContainerRunSpecification(
        name = "availe-ejabberd",
        image = "ghcr.io/processone/ejabberd",
        tag = "25.10",
        environmentVariables = emptyList(),
        portMappings = EjabberdPortsList,
        volumes = EjabberdVolumesList(configurationTemporaryFile.absolutePath)
    )
}

private fun createEjabberdConfigFileOnHost(
    ejabberdConfiguration: EjabberdConfig,
    postgresConfiguration: PostgresConfig
): File {
    val resourceStream = object {}.javaClass.getResourceAsStream("/ejabberd.yml")
        ?: error("Could not find /ejabberd.yml in resources")

    val templateContent = resourceStream.bufferedReader().use { it.readText() }

    val databaseName = postgresConfiguration.dbUrl.substringAfterLast('/')
    val internalKeycloakUrl = "http://availe-keycloak:${KeycloakPortsList.Http.containerPortNumber}"
    val realmName = SharedAuthenticationConstants.REALM_NAME
    val protocolPath = SharedAuthenticationConstants.Paths.OPENID_CONNECT_CERTIFICATES
    val keycloakJwksUrl = "$internalKeycloakUrl/realms/$realmName$protocolPath"

    val substitutedContent = templateContent
        .replace("@HOST@", SharedNetworkConstants.EJABBERD_HOST)
        .replace("@ADMIN_USER@", ejabberdConfiguration.adminUser)
        .replace("@DB_SERVER@", "availe-postgres")
        .replace("@DB_DATABASE@", databaseName)
        .replace("@DB_USERNAME@", postgresConfiguration.dbUser)
        .replace("@DB_PASSWORD@", postgresConfiguration.dbPassword)
        .replace("@KEYCLOAK_URL@", keycloakJwksUrl)

    val temporaryFile = File.createTempFile("ejabberd-", ".yml")
    temporaryFile.deleteOnExit()
    temporaryFile.writeText(substitutedContent)

    return temporaryFile
}