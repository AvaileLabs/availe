package com.availelabs.provisioning.keycloak.reconcilers

import com.availelabs.provisioning.keycloak.specs.DesiredClient
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.ClientRepresentation
import org.slf4j.LoggerFactory

internal class ClientsReconciler {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun reconcile(realmResource: RealmResource, desiredClients: List<DesiredClient>) {
        logger.info("Reconciling clients...")
        val clientsResource = realmResource.clients()
        val existingClients = clientsResource.findAll().associateBy { it.clientId }

        desiredClients.forEach { desired ->
            val existing = existingClients[desired.clientId]

            if (existing == null) {
                createClient(clientsResource, desired)
            } else {
                updateClientIfChanged(clientsResource, existing, desired)
            }
        }
        logger.info("Client reconciliation complete.")
    }

    private fun createClient(
        clientsResource: org.keycloak.admin.client.resource.ClientsResource,
        desired: DesiredClient
    ) {
        logger.info("Client '{}' not found. Creating...", desired.clientId)
        val newClient = ClientRepresentation().apply {
            clientId = desired.clientId
            name = desired.clientName
            isPublicClient = desired.isPublic
            isStandardFlowEnabled = desired.isStandardFlowEnabled
            isDirectAccessGrantsEnabled = desired.isDirectAccessGrantsEnabled
            redirectUris = desired.redirectUris.toList()
            webOrigins = desired.webOrigins.toList()
            defaultClientScopes = desired.defaultClientScopes
            attributes = desired.attributes.toMutableMap()
        }
        clientsResource.create(newClient)
        logger.info("Client '{}' created.", desired.clientId)
    }

    private fun updateClientIfChanged(
        clientsResource: org.keycloak.admin.client.resource.ClientsResource,
        existing: ClientRepresentation,
        desired: DesiredClient
    ) {
        var changed = false

        fun <T> sync(fieldName: String, current: T?, target: T, setter: (T) -> Unit) {
            if (current != target) {
                setter(target)
                logger.info(
                    "Client '{}': Field '{}' changed. Old: '{}', New: '{}'",
                    desired.clientId,
                    fieldName,
                    current,
                    target
                )
                changed = true
            }
        }

        fun syncSet(fieldName: String, current: List<String>?, target: Set<String>, setter: (List<String>) -> Unit) {
            val currentSet = current?.toSet() ?: emptySet()
            if (currentSet != target) {
                setter(target.toList())
                logger.info("Client '{}': Field '{}' changed.", desired.clientId, fieldName)
                changed = true
            }
        }

        sync("Name", existing.name, desired.clientName) { existing.name = it }
        sync("IsPublic", existing.isPublicClient, desired.isPublic) { existing.isPublicClient = it }
        sync(
            "StandardFlow",
            existing.isStandardFlowEnabled,
            desired.isStandardFlowEnabled
        ) { existing.isStandardFlowEnabled = it }
        sync(
            "DirectAccess",
            existing.isDirectAccessGrantsEnabled,
            desired.isDirectAccessGrantsEnabled
        ) { existing.isDirectAccessGrantsEnabled = it }

        syncSet("RedirectUris", existing.redirectUris, desired.redirectUris) { existing.redirectUris = it }
        syncSet("WebOrigins", existing.webOrigins, desired.webOrigins) { existing.webOrigins = it }

        val currentScopes = existing.defaultClientScopes?.toSet() ?: emptySet()
        val targetScopes = desired.defaultClientScopes.toSet()
        if (currentScopes != targetScopes) {
            existing.defaultClientScopes = desired.defaultClientScopes
            logger.info("Client '{}': Field 'DefaultClientScopes' changed.", desired.clientId)
            changed = true
        }

        sync("Attributes", existing.attributes, desired.attributes) { existing.attributes = it.toMutableMap() }

        if (changed) {
            logger.info("Updating client '{}'...", desired.clientId)
            clientsResource.get(existing.id).update(existing)
        } else {
            logger.info("Client '{}' is up to date.", desired.clientId)
        }
    }
}