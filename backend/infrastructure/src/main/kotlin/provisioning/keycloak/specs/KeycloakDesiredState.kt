package com.availelabs.provisioning.keycloak.specs

import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.provisioning.keycloak.specs.DesiredClient
import com.availelabs.availe.provisioning.keycloak.specs.DesiredRealmSettings
import org.keycloak.representations.idm.ProtocolMapperRepresentation

internal object KeycloakDesiredState {
    const val REALM_NAME = KeycloakRealms.AVAILE
    const val DB_SCHEMA_NAME = KeycloakDbSchemas.KEYCLOAK

    val REALM_SETTINGS = DesiredRealmSettings()

    private const val HOST_IP = NetworkConstants.HOST_IP

    private val webAppRedirects = setOf(
        "http://$HOST_IP:3000/*",
        "https://$HOST_IP:3000/*",
        "http://$HOST_IP:8080/*"
    )

    private val mobileAppRedirects = setOf(
        "com.availe.auth://callback"
    )

    private val serviceRedirects = setOf(
        "http://$HOST_IP:8888/callback",
        "http://127.0.0.1:8888/callback"
    )

    private val allRedirectUris = webAppRedirects + mobileAppRedirects + serviceRedirects

    private val allWebOrigins = setOf(
        "http://$HOST_IP:3000",
        "https://$HOST_IP:3000",
        "http://$HOST_IP:8080"
    )

    private val userIdMapper = ProtocolMapperRepresentation().apply {
        name = "user-id-mapper"
        protocol = "openid-connect"
        protocolMapper = "oidc-usermodel-property-mapper"
        config = mapOf(
            "user.attribute" to "id",
            "claim.name" to "user_id",
            "jsonType.label" to "String",
            "id.token.claim" to "true",
            "access.token.claim" to "true"
        )
    }

    val CLIENTS = listOf(
        DesiredClient(
            clientId = KeycloakClientIds.APP,
            clientName = "Availe App",
            isPublic = true,
            redirectUris = allRedirectUris,
            webOrigins = allWebOrigins,
            defaultClientScopes = listOf(
                KeycloakClientScopes.EMAIL,
                KeycloakClientScopes.ROLES,
                KeycloakClientScopes.WEB_ORIGINS
            ),
            optionalClientScopes = listOf(
                KeycloakClientScopes.OFFLINE_ACCESS
            ),
            protocolMappers = listOf(userIdMapper)
        )
    )

    val USER_PROFILE_ATTRIBUTES_TO_REMOVE = listOf(
        KeycloakUserAttributes.FIRST_NAME,
        KeycloakUserAttributes.LAST_NAME
    )
}