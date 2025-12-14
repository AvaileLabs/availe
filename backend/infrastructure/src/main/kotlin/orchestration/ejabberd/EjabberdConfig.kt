package com.availelabs.orchestration.ejabberd

import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.orchestration.keycloak.KeycloakPortsList
import com.availelabs.orchestration.postgres.loadPostgresConfig
import com.availelabs.utils.TlsCertificateGenerator
import com.availelabs.utils.loadGradleProperties
import com.fasterxml.jackson.databind.ObjectMapper
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
    val projectRootDirectory = System.getProperty("user.dir")
    val generatedConfigDirectory = File(projectRootDirectory, "build/generated/ejabberd")
    val configFile = File(generatedConfigDirectory, "ejabberd.yml")
    val pemFile = File(generatedConfigDirectory, "server.pem")
    val jwkFile = File(generatedConfigDirectory, "keycloak.jwk")

    /**
     * We include the host IP in the environment variables solely to trigger the
     * drift detection logic in DockerContainerManager when the IP changes.
     * Ejabberd doesn't use this directly, but it still forces it to reload the correct IP.
     */
    val environmentVariables = listOf(
        "AVAILE_HOST_IP_TRACKER=${NetworkConstants.HOST_IP}"
    )

    return ContainerRunSpecification(
        name = "availe-ejabberd",
        image = "ghcr.io/processone/ejabberd",
        tag = "25.10",
        environmentVariables = environmentVariables,
        portMappings = EjabberdPortsList,
        volumes = EjabberdVolumesList(
            configHostPath = configFile.absolutePath,
            pemHostPath = pemFile.absolutePath,
            jwkHostPath = jwkFile.absolutePath
        ),
        logStrategy = ContainerRunSpecification.LogStrategy.STREAM_ALWAYS
    )
}

internal fun generateEjabberdConfigurationFiles() {
    val ejabberdConfiguration = loadEjabberdConfig()
    val postgresConfiguration = loadPostgresConfig()

    val projectRootDirectory = System.getProperty("user.dir")
    val generatedConfigDirectory = File(projectRootDirectory, "build/generated/ejabberd")

    if (!generatedConfigDirectory.exists()) {
        generatedConfigDirectory.mkdirs()
    }

    val pemContent = TlsCertificateGenerator.generateSelfSignedCertificatePem("localhost")
    val pemFile = File(generatedConfigDirectory, "server.pem")
    pemFile.writeText(pemContent)

    val jwksContent = fetchKeycloakJwks()

    val objectMapper = ObjectMapper()
    val rootNode = objectMapper.readTree(jwksContent)
    val keysNode = rootNode.get("keys")
        ?: error("Invalid JWKS response from Keycloak: 'keys' array not found")

    val signingKeyNode = keysNode.firstOrNull { it.get("use")?.asText() == "sig" }
        ?: keysNode.firstOrNull()
        ?: error("No keys found in Keycloak JWKS")

    val singleJwkContent = objectMapper.writeValueAsString(signingKeyNode)

    val jwkFile = File(generatedConfigDirectory, "keycloak.jwk")
    jwkFile.writeText(singleJwkContent)

    val resourceStream = object {}.javaClass.getResourceAsStream("/ejabberd.yml")
        ?: error("Could not find /ejabberd.yml in resources")

    val templateContent = resourceStream.bufferedReader().use { it.readText() }

    val databaseName = postgresConfiguration.dbUrl.substringAfterLast('/')

    val containerJwkPath = "/opt/ejabberd/conf/keycloak.jwk"

    val substitutedContent = templateContent
        .replace("@HOST@", SharedNetworkConstants.EJABBERD_HOST)
        .replace("@ADMIN_USER@", ejabberdConfiguration.adminUser)
        .replace("@DB_SERVER@", "availe-postgres")
        .replace("@DB_DATABASE@", databaseName)
        .replace("@DB_USERNAME@", postgresConfiguration.dbUser)
        .replace("@DB_PASSWORD@", postgresConfiguration.dbPassword)
        .replace("@KEYCLOAK_URL@", containerJwkPath)

    val configFile = File(generatedConfigDirectory, "ejabberd.yml")
    configFile.writeText(substitutedContent)
}

private fun fetchKeycloakJwks(): String {
    val host = "localhost"
    val port = KeycloakPortsList.Http.hostPortNumber
    val realm = com.availelabs.availe.configuration.SharedAuthenticationConstants.REALM_NAME
    val url =
        "http://$host:$port/realms/$realm${com.availelabs.availe.configuration.SharedAuthenticationConstants.Paths.OPENID_CONNECT_CERTIFICATES}"

    val client = java.net.http.HttpClient.newHttpClient()
    val request = java.net.http.HttpRequest.newBuilder()
        .uri(java.net.URI.create(url))
        .GET()
        .build()

    val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())

    if (response.statusCode() != 200) {
        throw IllegalStateException("Failed to fetch Keycloak JWKS. Status: ${response.statusCode()} Body: ${response.body()}")
    }

    return response.body()
}