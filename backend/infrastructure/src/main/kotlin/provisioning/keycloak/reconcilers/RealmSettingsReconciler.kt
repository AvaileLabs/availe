package com.availelabs.provisioning.keycloak.reconcilers

import com.availelabs.provisioning.keycloak.specs.DesiredRealmSettings
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.RealmRepresentation
import org.slf4j.LoggerFactory

internal class RealmSettingsReconciler {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var needsUpdate = false

    private inline fun <T> reconcileProperty(
        existingRealm: RealmRepresentation,
        currentValue: T,
        desiredValue: T,
        setter: RealmRepresentation.(T) -> Unit
    ) {
        if (currentValue != desiredValue) {
            existingRealm.setter(desiredValue)
            needsUpdate = true
        }
    }

    fun reconcile(realmResource: RealmResource, desired: DesiredRealmSettings) {
        val existingRealm = realmResource.toRepresentation()
        needsUpdate = false

        reconcileProperty(
            existingRealm,
            existingRealm.accessTokenLifespan,
            desired.accessTokenLifespan
        ) { accessTokenLifespan = it }
        reconcileProperty(
            existingRealm,
            existingRealm.ssoSessionIdleTimeout,
            desired.ssoSessionIdleTimeout
        ) { ssoSessionIdleTimeout = it }
        reconcileProperty(
            existingRealm,
            existingRealm.ssoSessionMaxLifespan,
            desired.ssoSessionMaxLifespan
        ) { ssoSessionMaxLifespan = it }
        reconcileProperty(
            existingRealm,
            existingRealm.offlineSessionIdleTimeout,
            desired.offlineSessionIdleTimeout
        ) { offlineSessionIdleTimeout = it }

        reconcileProperty(
            existingRealm,
            existingRealm.isRegistrationAllowed,
            desired.registrationAllowed
        ) { isRegistrationAllowed = it }
        reconcileProperty(
            existingRealm,
            existingRealm.isResetPasswordAllowed,
            desired.resetPasswordAllowed
        ) { isResetPasswordAllowed = it }
        reconcileProperty(existingRealm, existingRealm.isRememberMe, desired.rememberMe) { isRememberMe = it }
        reconcileProperty(
            existingRealm,
            existingRealm.isLoginWithEmailAllowed,
            desired.loginWithEmailAllowed
        ) { isLoginWithEmailAllowed = it }
        reconcileProperty(
            existingRealm,
            existingRealm.isRegistrationEmailAsUsername,
            desired.registrationEmailAsUsername
        ) { isRegistrationEmailAsUsername = it }
        reconcileProperty(
            existingRealm,
            existingRealm.isDuplicateEmailsAllowed,
            desired.duplicateEmailsAllowed
        ) { isDuplicateEmailsAllowed = it }
        reconcileProperty(existingRealm, existingRealm.isVerifyEmail, desired.verifyEmail) { isVerifyEmail = it }

        reconcileProperty(
            existingRealm,
            existingRealm.isBruteForceProtected,
            desired.bruteForceProtected
        ) { isBruteForceProtected = it }
        reconcileProperty(
            existingRealm,
            existingRealm.isPermanentLockout,
            desired.permanentLockout
        ) { isPermanentLockout = it }
        reconcileProperty(
            existingRealm,
            existingRealm.maxFailureWaitSeconds,
            desired.maxFailureWaitSeconds
        ) { maxFailureWaitSeconds = it }
        reconcileProperty(existingRealm, existingRealm.failureFactor, desired.failureFactor) { failureFactor = it }

        if (needsUpdate) {
            logger.info("Realm settings are out of sync. Updating...")
            realmResource.update(existingRealm)
            logger.info("Realm settings updated.")
        } else {
            logger.info("Realm settings are up to date.")
        }
    }
}