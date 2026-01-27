package provisioning.keycloak.reconcilers

import com.availelabs.provisioning.keycloak.specs.DesiredRealmSettings
import org.keycloak.admin.client.resource.RealmResource
import org.slf4j.LoggerFactory

internal class RealmSettingsReconciler {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun reconcile(realmResource: RealmResource, desired: DesiredRealmSettings) {
        val existingRealm = realmResource.toRepresentation()
        var changed = false

        fun <T> sync(fieldName: String, current: T?, target: T, setter: (T) -> Unit) {
            if (current != target) {
                setter(target)
                logger.info("Realm setting '{}' changed. Old: '{}', New: '{}'", fieldName, current, target)
                changed = true
            }
        }

        fun syncList(fieldName: String, current: List<String>?, target: List<String>, setter: (List<String>) -> Unit) {
            val currentList = current ?: emptyList()
            if (currentList.toSet() != target.toSet()) {
                setter(target)
                logger.info("Realm setting '{}' changed. Old: '{}', New: '{}'", fieldName, current, target)
                changed = true
            }
        }

        sync(
            "AccessTokenLifespan",
            existingRealm.accessTokenLifespan,
            desired.accessTokenLifespan
        ) { existingRealm.accessTokenLifespan = it }
        sync(
            "SsoSessionIdleTimeout",
            existingRealm.ssoSessionIdleTimeout,
            desired.ssoSessionIdleTimeout
        ) { existingRealm.ssoSessionIdleTimeout = it }
        sync(
            "SsoSessionMaxLifespan",
            existingRealm.ssoSessionMaxLifespan,
            desired.ssoSessionMaxLifespan
        ) { existingRealm.ssoSessionMaxLifespan = it }
        sync(
            "OfflineSessionIdleTimeout",
            existingRealm.offlineSessionIdleTimeout,
            desired.offlineSessionIdleTimeout
        ) { existingRealm.offlineSessionIdleTimeout = it }

        sync(
            "RegistrationAllowed",
            existingRealm.isRegistrationAllowed,
            desired.registrationAllowed
        ) { existingRealm.isRegistrationAllowed = it }
        sync(
            "ResetPasswordAllowed",
            existingRealm.isResetPasswordAllowed,
            desired.resetPasswordAllowed
        ) { existingRealm.isResetPasswordAllowed = it }
        sync("RememberMe", existingRealm.isRememberMe, desired.rememberMe) { existingRealm.isRememberMe = it }
        sync(
            "LoginWithEmailAllowed",
            existingRealm.isLoginWithEmailAllowed,
            desired.loginWithEmailAllowed
        ) { existingRealm.isLoginWithEmailAllowed = it }
        sync(
            "RegistrationEmailAsUsername",
            existingRealm.isRegistrationEmailAsUsername,
            desired.registrationEmailAsUsername
        ) { existingRealm.isRegistrationEmailAsUsername = it }
        sync(
            "DuplicateEmailsAllowed",
            existingRealm.isDuplicateEmailsAllowed,
            desired.duplicateEmailsAllowed
        ) { existingRealm.isDuplicateEmailsAllowed = it }
        sync("VerifyEmail", existingRealm.isVerifyEmail, desired.verifyEmail) { existingRealm.isVerifyEmail = it }

        sync(
            "BruteForceProtected",
            existingRealm.isBruteForceProtected,
            desired.bruteForceProtected
        ) { existingRealm.isBruteForceProtected = it }
        sync(
            "PermanentLockout",
            existingRealm.isPermanentLockout,
            desired.permanentLockout
        ) { existingRealm.isPermanentLockout = it }
        sync(
            "MaxFailureWaitSeconds",
            existingRealm.maxFailureWaitSeconds,
            desired.maxFailureWaitSeconds
        ) { existingRealm.maxFailureWaitSeconds = it }
        sync("FailureFactor", existingRealm.failureFactor, desired.failureFactor) { existingRealm.failureFactor = it }

        sync("EventsEnabled", existingRealm.isEventsEnabled, desired.eventsEnabled) {
            existingRealm.isEventsEnabled = it
        }
        syncList(
            "EventsListeners",
            existingRealm.eventsListeners,
            desired.eventsListeners
        ) { existingRealm.eventsListeners = it }

        if (changed) {
            logger.info("Realm settings are out of sync. Updating...")
            realmResource.update(existingRealm)
            logger.info("Realm settings updated.")
        } else {
            logger.info("Realm settings are up to date.")
        }
    }
}