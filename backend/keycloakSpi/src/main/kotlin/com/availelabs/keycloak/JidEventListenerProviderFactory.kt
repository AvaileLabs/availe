package com.availelabs.keycloak

import org.keycloak.Config
import org.keycloak.events.EventListenerProvider
import org.keycloak.events.EventListenerProviderFactory
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory

class JidEventListenerProviderFactory : EventListenerProviderFactory {

    override fun create(keycloakSession: KeycloakSession): EventListenerProvider {
        return JidEventListenerProvider(keycloakSession)
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