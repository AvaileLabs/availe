package com.availelabs.provisioners

import com.availelabs.provisioners.spec.DesiredClient
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.ClientRepresentation
import org.slf4j.LoggerFactory

internal class ClientsReconciler {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun reconcile(realmResource: RealmResource, desiredClients: List<DesiredClient>) {
        logger.info("Reconciling clients...")
        val clientsResource = realmResource.clients()
        val existingClients = clientsResource.findAll().associateBy { it.clientId }

        desiredClients.forEach { desiredClient ->
            val existingClient = existingClients[desiredClient.clientId]
            if (existingClient == null) {
                logger.info("Client '{}' not found. Creating...", desiredClient.clientId)
                val newClient = ClientRepresentation().apply {
                    clientId = desiredClient.clientId
                    name = desiredClient.clientName
                    isPublicClient = desiredClient.isPublic
                    isStandardFlowEnabled = desiredClient.isStandardFlowEnabled
                    isDirectAccessGrantsEnabled = desiredClient.isDirectAccessGrantsEnabled
                    redirectUris = desiredClient.redirectUris.toList()
                    webOrigins = desiredClient.webOrigins.toList()
                    defaultClientScopes = desiredClient.defaultClientScopes
                    attributes = desiredClient.attributes.toMutableMap()
                }
                clientsResource.create(newClient)
                logger.info("Client '{}' created.", desiredClient.clientId)
                return@forEach
            }

            val currentScopes = existingClient.defaultClientScopes ?: emptyList()
            val existingAttrs = existingClient.attributes ?: emptyMap()

            val needsUpdate =
                existingClient.redirectUris.toSet() != desiredClient.redirectUris ||
                        existingClient.webOrigins.toSet() != desiredClient.webOrigins ||
                        currentScopes.toSet() != desiredClient.defaultClientScopes.toSet() ||
                        existingAttrs != desiredClient.attributes ||
                        existingClient.isPublicClient != desiredClient.isPublic ||
                        existingClient.isStandardFlowEnabled != desiredClient.isStandardFlowEnabled ||
                        existingClient.isDirectAccessGrantsEnabled != desiredClient.isDirectAccessGrantsEnabled

            if (needsUpdate) {
                logger.info("Client '{}' is out of sync. Updating...", desiredClient.clientId)
                existingClient.redirectUris = desiredClient.redirectUris.toList()
                existingClient.webOrigins = desiredClient.webOrigins.toList()
                existingClient.defaultClientScopes = desiredClient.defaultClientScopes
                existingClient.attributes = desiredClient.attributes.toMutableMap()
                existingClient.isPublicClient = desiredClient.isPublic
                existingClient.isStandardFlowEnabled = desiredClient.isStandardFlowEnabled
                existingClient.isDirectAccessGrantsEnabled = desiredClient.isDirectAccessGrantsEnabled

                clientsResource.get(existingClient.id).update(existingClient)
                logger.info("Client '{}' updated.", desiredClient.clientId)
            } else {
                logger.info("Client '{}' is up to date.", desiredClient.clientId)
            }
        }
        logger.info("Client reconciliation complete.")
    }
}