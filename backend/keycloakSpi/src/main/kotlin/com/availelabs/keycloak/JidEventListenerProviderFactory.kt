package com.availelabs.keycloak

import com.availelabs.availe.NetworkConstants
import org.keycloak.Config
import org.keycloak.events.EventListenerProvider
import org.keycloak.events.EventListenerProviderFactory
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory

class JidEventListenerProviderFactory : EventListenerProviderFactory {
    private val configuration = JidEventListenerConfiguration(
        xmppDomain = XmppDomain(NetworkConstants.XMPP_DOMAIN)
    )

    override fun create(keycloakSession: KeycloakSession): EventListenerProvider {
        return JidEventListenerProvider(
            keycloakSession = keycloakSession,
            configuration = configuration
        )
    }

    override fun init(scope: Config.Scope) {
    }

    override fun postInit(keycloakSessionFactory: KeycloakSessionFactory) {
    }

    override fun close() {
    }

    override fun getId(): String {
        return "jid-listener"
    }
}