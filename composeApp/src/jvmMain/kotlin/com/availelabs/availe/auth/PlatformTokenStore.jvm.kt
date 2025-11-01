package com.availelabs.availe.auth

import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.tokenstore.SettingsStore
import org.publicvalue.multiplatform.oidc.tokenstore.SettingsTokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore

@OptIn(ExperimentalOpenIdConnect::class)
private class JvmPreferencesSettingsStore : SettingsStore {
    private val delegate: Settings = Settings()

    override suspend fun clear() = withContext(Dispatchers.IO) {
        delegate.clear()
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        delegate.getStringOrNull(key)
    }

    override suspend fun put(key: String, value: String) = withContext(Dispatchers.IO) {
        delegate.putString(key, value)
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        delegate.remove(key)
    }
}

@OptIn(ExperimentalOpenIdConnect::class)
actual fun createTokenStore(activity: PlatformActivity): TokenStore {
    return SettingsTokenStore(JvmPreferencesSettingsStore())
}