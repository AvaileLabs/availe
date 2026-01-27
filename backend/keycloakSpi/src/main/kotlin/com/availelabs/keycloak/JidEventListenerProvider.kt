package com.availelabs.keycloak

import org.jboss.logging.Logger
import org.keycloak.events.Event
import org.keycloak.events.EventListenerProvider
import org.keycloak.events.EventType
import org.keycloak.events.admin.AdminEvent
import org.keycloak.events.admin.OperationType
import org.keycloak.events.admin.ResourceType
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.models.UserModel

class JidEventListenerProvider(private val keycloakSession: KeycloakSession) : EventListenerProvider {

    private val logger = Logger.getLogger(JidEventListenerProvider::class.java)

    override fun onEvent(event: Event) {
        if (event.type == EventType.REGISTER) {
            handleRegisterEvent(event)
        }
    }

    override fun onEvent(adminEvent: AdminEvent, includeRepresentation: Boolean) {
        if (adminEvent.operationType == OperationType.CREATE && adminEvent.resourceType == ResourceType.USER) {
            handleAdminUserCreation(adminEvent)
        }
    }

    override fun close() {
    }

    private fun handleRegisterEvent(event: Event) {
        val userId = event.userId
        val realmId = event.realmId

        assignJidToUser(realmId, userId)
    }

    private fun handleAdminUserCreation(adminEvent: AdminEvent) {
        val realmId = adminEvent.realmId
        val resourcePath = adminEvent.resourcePath

        if (resourcePath == null || !resourcePath.startsWith("users/")) {
            logger.warnf("Invalid resource path for user creation event: %s", resourcePath)
            return
        }

        val userId = resourcePath.substringAfter("users/")
        assignJidToUser(realmId, userId)
    }

    private fun assignJidToUser(realmId: String, userId: String) {
        val realmModel: RealmModel? = keycloakSession.realms().getRealm(realmId)

        if (realmModel == null) {
            logger.errorf("Realm %s not found during JID generation", realmId)
            return
        }

        keycloakSession.context.realm = realmModel
        val userModel: UserModel? = keycloakSession.users().getUserById(realmModel, userId)

        if (userModel != null) {
            val jidValue = "${userId}@localhost"
            userModel.setSingleAttribute("jid", jidValue)
            logger.infof("Successfully assigned JID %s to user %s", jidValue, userId)
        } else {
            logger.warnf("User %s not found in current session context", userId)
        }
    }
}