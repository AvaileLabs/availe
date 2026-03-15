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

@JvmInline
value class XmppDomain(val value: String) {
    init {
        require(value.isNotBlank())
    }
}

data class JidEventListenerConfiguration(
    val xmppDomain: XmppDomain
)

internal object JidAttributeAssigner {
    private const val JID_ATTRIBUTE_NAME = "jid"

    fun createJidValue(userId: String, configuration: JidEventListenerConfiguration): String {
        require(userId.isNotBlank())
        return "$userId@${configuration.xmppDomain.value}"
    }

    fun assignTo(userModel: UserModel, configuration: JidEventListenerConfiguration): String {
        val userId = requireNotNull(userModel.id)
        val jidValue = createJidValue(
            userId = userId,
            configuration = configuration
        )
        userModel.setSingleAttribute(JID_ATTRIBUTE_NAME, jidValue)
        return jidValue
    }
}

class JidEventListenerProvider(
    private val keycloakSession: KeycloakSession,
    private val configuration: JidEventListenerConfiguration
) : EventListenerProvider {

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
            val jidValue = JidAttributeAssigner.assignTo(
                userModel = userModel,
                configuration = configuration
            )
            logger.infof("Successfully assigned JID %s to user %s", jidValue, userId)
        } else {
            logger.warnf("User %s not found in current session context", userId)
        }
    }
}