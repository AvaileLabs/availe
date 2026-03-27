package com.availelabs.orchestration.ejabberd

import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.configuration.SharedNetworkConstants
import com.availelabs.orchestration.InfrastructureRegistry
import com.availelabs.orchestration.containers.domain.ContainerRunSpecification
import com.availelabs.utils.ServiceWaiter
import com.availelabs.utils.TlsCertificateGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

internal fun ejabberdRunSpec(
    serviceWaiter: ServiceWaiter
): ContainerRunSpecification<EjabberdPortsList, EjabberdVolumesList> {
    val projectRootDirectory = System.getProperty("user.dir")
    val generatedConfigDirectory = File(projectRootDirectory, "build/generated/ejabberd")
    val configFile = File(generatedConfigDirectory, "ejabberd.yml")
    val pemFile = File(generatedConfigDirectory, "server.pem")
    val jwkFile = File(generatedConfigDirectory, "keycloak.jwk")

    val environmentVariables = listOf(
        "AVAILE_HOST_IP_TRACKER=${NetworkConstants.HOST_IP}",
        "ERLANG_COOKIE=availe-dev-cookie"
    )

    return ContainerRunSpecification(
        name = InfrastructureRegistry.Topology.EJABBERD_CONTAINER_NAME,
        image = InfrastructureRegistry.Images.EJABBERD_REPOSITORY,
        tag = InfrastructureRegistry.Images.EJABBERD_TAG,
        environmentVariables = environmentVariables,
        portMappings = EjabberdPortsList,
        volumes = EjabberdVolumesList(
            configHostPath = configFile.absolutePath,
            pemHostPath = pemFile.absolutePath,
            jwkHostPath = jwkFile.absolutePath
        ),
        logStrategy = ContainerRunSpecification.LogStrategy.BUFFER_AND_DUMP_ON_FAILURE,
        requiresDataBackup = true,
        containerHealthCheckVerificationAction = { serviceWaiter.waitForEjabberd(InfrastructureRegistry.Topology.EJABBERD_CONTAINER_NAME) }
    )
}

internal fun generateEjabberdConfigurationFiles() {
    val ejabberdConfiguration = loadEjabberdConfig()

    val projectRootDirectory = System.getProperty("user.dir")
    val generatedConfigDirectory = File(projectRootDirectory, "build/generated/ejabberd")

    if (!generatedConfigDirectory.exists()) {
        generatedConfigDirectory.mkdirs()
    }

    val pemContent = TlsCertificateGenerator.generateSelfSignedCertificatePem("localhost")
    val pemFile = File(generatedConfigDirectory, "server.pem")
    pemFile.writeText(pemContent)

    val jwksContent = fetchKeycloakJwks()

    val jwtKeyFileContent = generateEjabberdJwtKeyFileContent(jwksContent)

    val jwkFile = File(generatedConfigDirectory, "keycloak.jwk")
    jwkFile.writeText(jwtKeyFileContent)

    val resourceStream = object {}.javaClass.getResourceAsStream("/ejabberd.yml")
        ?: error("Could not find /ejabberd.yml in resources")

    val templateContent = resourceStream.bufferedReader().use { it.readText() }

    val databaseName = InfrastructureRegistry.Topology.EJABBERD_DATABASE_NAME
    val databaseHost = InfrastructureRegistry.Topology.POSTGRES_EJABBERD_CONTAINER_NAME
    val containerJwkPath = "/opt/ejabberd/conf/keycloak.jwk"

    val substitutedContent = templateContent
        .replace("@HOST@", SharedNetworkConstants.EJABBERD_HOST)
        .replace("@ADMIN_USER@", ejabberdConfiguration.adminUser)
        .replace("@DB_SERVER@", databaseHost)
        .replace("@DB_DATABASE@", databaseName)
        .replace("@DB_USERNAME@", ejabberdConfiguration.databaseUser)
        .replace("@DB_PASSWORD@", ejabberdConfiguration.databasePassword)
        .replace("@KEYCLOAK_URL@", containerJwkPath)

    val configFile = File(generatedConfigDirectory, "ejabberd.yml")
    configFile.writeText(substitutedContent)
}

internal fun generateEjabberdJwtKeyFileContent(keycloakJwksContent: String): String {
    val objectMapper = ObjectMapper()
    val rootNode = objectMapper.readTree(keycloakJwksContent)
    val keysNode = rootNode.get("keys")
        ?: error("Invalid JWKS response from Keycloak: 'keys' array not found")

    val signingKeys = keysNode.filter { it.get("use")?.asText() == "sig" }
    val selectedKeys = if (signingKeys.isNotEmpty()) signingKeys else keysNode.toList()
    if (selectedKeys.isEmpty()) error("No keys found in Keycloak JWKS")

    val jwksNode = objectMapper.createObjectNode()
    val keysArrayNode = objectMapper.createArrayNode()
    selectedKeys.forEach { keysArrayNode.add(it) }
    jwksNode.set<com.fasterxml.jackson.databind.JsonNode>("keys", keysArrayNode)
    return objectMapper.writeValueAsString(jwksNode)
}

private fun fetchKeycloakJwks(): String {
    val host = "localhost"
    val port = com.availelabs.orchestration.keycloak.KeycloakPortsList.Http.hostPortNumber
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