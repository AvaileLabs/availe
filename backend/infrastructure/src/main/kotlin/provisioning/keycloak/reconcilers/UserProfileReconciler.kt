package com.availelabs.provisioning.keycloak.reconcilers

import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.userprofile.config.UPConfig
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy
import org.slf4j.LoggerFactory

internal class UserProfileReconciler {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun reconcile(realmResource: RealmResource, attributesToRemove: List<String>) {
        logger.info("Reconciling User Profile...")
        val userProfileResource = realmResource.users().userProfile()

        try {
            val configuration: UPConfig = userProfileResource.configuration
            var hasConfigurationChanged = false

            // 1. Ensure Unmanaged Attribute Policy is ENABLED
            // This is required for the SPI to write the 'jid' attribute, which is not defined in the schema.
            if (configuration.unmanagedAttributePolicy != UnmanagedAttributePolicy.ENABLED) {
                logger.info(
                    "Unmanaged Attribute Policy changed. Old: '{}', New: '{}'",
                    configuration.unmanagedAttributePolicy,
                    UnmanagedAttributePolicy.ENABLED
                )
                configuration.unmanagedAttributePolicy = UnmanagedAttributePolicy.ENABLED
                hasConfigurationChanged = true
            }

            // 2. Remove specific attributes if they exist
            val currentAttributes = configuration.attributes ?: emptyList()
            val filteredAttributes = currentAttributes.filterNot { it.name in attributesToRemove }

            if (currentAttributes.size != filteredAttributes.size) {
                logger.info("Removing attributes from User Profile: {}", attributesToRemove)
                configuration.attributes = filteredAttributes
                hasConfigurationChanged = true
            }

            if (hasConfigurationChanged) {
                userProfileResource.update(configuration)
                logger.info("User Profile successfully updated.")
            } else {
                logger.info("User Profile is already up to date.")
            }

        } catch (exception: Exception) {
            logger.error("Failed to reconcile User Profile: ${exception.message}", exception)
            throw IllegalStateException("Could not update User Profile", exception)
        }
    }
}