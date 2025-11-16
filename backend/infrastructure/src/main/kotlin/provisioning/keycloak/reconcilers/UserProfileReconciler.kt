package com.availelabs.provisioning.keycloak.reconcilers

import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.userprofile.config.UPConfig
import org.slf4j.LoggerFactory

internal class UserProfileReconciler {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun reconcile(realmResource: RealmResource, attributesToRemove: List<String>) {
        logger.info("Reconciling User Profile (removing attributes: $attributesToRemove)...")
        val userProfileResource = realmResource.users().userProfile()

        try {
            val currentConfig: UPConfig = userProfileResource.configuration

            val originalAttributeCount = currentConfig.attributes?.size ?: 0

            val newAttributes = currentConfig.attributes?.filterNot {
                it.name in attributesToRemove
            }

            if (newAttributes == null || newAttributes.size == originalAttributeCount) {
                logger.info("User Profile is already up to date. No attributes needed to be removed.")
                return
            }

            val newConfig = UPConfig()
            newConfig.attributes = newAttributes

            userProfileResource.update(newConfig)
            logger.info("Successfully updated User Profile. Removed attributes: $attributesToRemove")

        } catch (e: Exception) {
            logger.error("Failed to reconcile User Profile: ${e.message}", e)
            throw IllegalStateException("Could not update User Profile", e)
        }
    }
}