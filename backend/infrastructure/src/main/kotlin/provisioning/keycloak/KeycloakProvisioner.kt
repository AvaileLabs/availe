package com.availelabs.provisioning.keycloak

import com.availelabs.orchestration.keycloak.KeycloakConfig
import com.availelabs.orchestration.keycloak.KeycloakPortsList
import com.availelabs.provisioning.keycloak.reconcilers.ClientsReconciler
import com.availelabs.provisioning.keycloak.reconcilers.UserProfileReconciler
import com.availelabs.provisioning.keycloak.specs.KeycloakDesiredState
import jakarta.ws.rs.NotFoundException
import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.RealmRepresentation
import org.slf4j.LoggerFactory
import provisioning.keycloak.reconcilers.RealmSettingsReconciler

internal class KeycloakProvisioner(
    private val config: KeycloakConfig,
    private val realmSettingsReconciler: RealmSettingsReconciler,
    private val clientsReconciler: ClientsReconciler,
    private val userProfileReconciler: UserProfileReconciler
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun provision() {
        logger.info("Provisioning Keycloak...")

        val keycloak = KeycloakBuilder.builder()
            .serverUrl("http://localhost:${KeycloakPortsList.Http.hostPortNumber}")
            .realm("master")
            .grantType(OAuth2Constants.PASSWORD)
            .clientId("admin-cli")
            .username(config.adminUser)
            .password(config.adminPassword)
            .build()

        keycloak.use {
            ensureRealmExists(it)
            val realmResource = it.realm(KeycloakDesiredState.REALM_NAME)

            realmSettingsReconciler.reconcile(realmResource, KeycloakDesiredState.REALM_SETTINGS)
            clientsReconciler.reconcile(realmResource, KeycloakDesiredState.CLIENTS)
            userProfileReconciler.reconcile(realmResource, KeycloakDesiredState.USER_PROFILE_ATTRIBUTES_TO_REMOVE)
        }
        logger.info("Keycloak provisioning complete.")
    }

    private fun ensureRealmExists(keycloak: Keycloak) {
        try {
            keycloak.realm(KeycloakDesiredState.REALM_NAME).toRepresentation()
            logger.info("Realm '{}' already exists.", KeycloakDesiredState.REALM_NAME)
        } catch (_: NotFoundException) {
            logger.info("Realm '{}' not found. Creating it...", KeycloakDesiredState.REALM_NAME)
            val newRealm = RealmRepresentation().apply {
                realm = KeycloakDesiredState.REALM_NAME
                isEnabled = true
            }
            keycloak.realms().create(newRealm)
            logger.info("Realm '{}' created.", KeycloakDesiredState.REALM_NAME)
        }
    }
}