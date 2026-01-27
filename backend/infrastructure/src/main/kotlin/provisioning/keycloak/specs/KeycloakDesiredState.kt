package com.availelabs.provisioning.keycloak.specs

import com.availelabs.availe.NetworkConstants
import com.availelabs.availe.configuration.SharedAuthenticationConstants
import org.keycloak.representations.idm.ProtocolMapperRepresentation

internal object KeycloakDesiredState {
    const val REALM_NAME = KeycloakRealms.AVAILE
    const val DB_SCHEMA_NAME = KeycloakDbSchemas.KEYCLOAK

    val REALM_SETTINGS = DesiredRealmSettings(
        eventsEnabled = true,
        eventsListeners = listOf("jboss-logging", "jid-listener")
    )

    private const val HOST_IP = NetworkConstants.HOST_IP

    private val webAppRedirects = setOf(
        "http://$HOST_IP:3000/*",
        "https://$HOST_IP:3000/*",
        "http://$HOST_IP:8080/*"
    )

    private val mobileAppRedirects = setOf(
        "${SharedAuthenticationConstants.REDIRECT_SCHEME}://callback"
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

    private val userIdProtocolMapper = ProtocolMapperRepresentation().apply {
        name = "user-id-mapper"
        protocol = "openid-connect"
        protocolMapper = "oidc-usermodel-property-mapper"
        config = mapOf(
            "user.attribute" to "id",
            "claim.name" to SharedAuthenticationConstants.Claims.USER_ID,
            "jsonType.label" to "String",
            "id.token.claim" to "true",
            "access.token.claim" to "true"
        )
    }

    private val jidAttributeMapper = ProtocolMapperRepresentation().apply {
        name = "user-jid-attribute-mapper"
        protocol = "openid-connect"
        protocolMapper = "oidc-usermodel-attribute-mapper"
        config = mapOf(
            "user.attribute" to "jid",
            "claim.name" to SharedAuthenticationConstants.Claims.JID,
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
                KeycloakClientScopes.PROFILE,
                KeycloakClientScopes.ROLES,
                KeycloakClientScopes.WEB_ORIGINS
            ),
            optionalClientScopes = listOf(
                KeycloakClientScopes.OFFLINE_ACCESS
            ),
            protocolMappers = listOf(
                userIdProtocolMapper,
                jidAttributeMapper
            )
        )
    )

    val USER_PROFILE_ATTRIBUTES_TO_REMOVE = listOf(
        KeycloakUserAttributes.FIRST_NAME,
        KeycloakUserAttributes.LAST_NAME
    )
}