package com.availelabs.keycloak

import org.jboss.logging.Logger
import org.keycloak.events.Event
import org.keycloak.events.EventListenerProvider
import org.keycloak.events.EventType
import org.keycloak.events.admin.AdminEvent
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.models.utils.KeycloakModelUtils

class JidEventListenerProvider(private val keycloakSession: KeycloakSession) : EventListenerProvider {

    private val logger = Logger.getLogger(JidEventListenerProvider::class.java)

    override fun onEvent(event: Event) {
        if (event.type == EventType.REGISTER) {
            handleRegisterEvent(event)
        }
    }

    override fun onEvent(adminEvent: AdminEvent, includeRepresentation: Boolean) {
    }

    override fun close() {
    }

    private fun handleRegisterEvent(event: Event) {
        val userId = event.userId
        val realmId = event.realmId

        logger.infof("Detected REGISTER event for user %s in realm %s", userId, realmId)

        KeycloakModelUtils.runJobInTransaction(keycloakSession.keycloakSessionFactory) { sessionInTransaction ->
            val realmModel: RealmModel? = sessionInTransaction.realms().getRealm(realmId)

            if (realmModel == null) {
                logger.errorf("Realm %s not found during JID generation", realmId)
                return@runJobInTransaction
            }

            sessionInTransaction.context.realm = realmModel

            val userModel = sessionInTransaction.users().getUserById(realmModel, userId)

            if (userModel != null) {
                val jidValue = "${userId}@localhost"
                userModel.setSingleAttribute("jid", jidValue)
                logger.infof("Successfully assigned JID %s to user %s", jidValue, userId)
            } else {
                logger.warnf("User %s not found in new transaction context", userId)
            }
        }
    }
}