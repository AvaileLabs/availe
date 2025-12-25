package com.availelabs.availe.auth

import com.availelabs.availe.platform.PlatformActivity
import com.russhwolf.settings.Settings
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.tokenstore.SettingsStore
import org.publicvalue.multiplatform.oidc.tokenstore.SettingsTokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore

@OptIn(ExperimentalOpenIdConnect::class)
private class WebLocalStorageSettingsStore : SettingsStore {
    private val delegate: Settings = Settings()

    override suspend fun clear() {
        delegate.clear()
    }

    override suspend fun get(key: String): String? {
        return delegate.getStringOrNull(key)
    }

    override suspend fun put(key: String, value: String) {
        delegate.putString(key, value)
    }

    override suspend fun remove(key: String) {
        delegate.remove(key)
    }
}

@OptIn(ExperimentalOpenIdConnect::class)
actual fun createTokenStore(activity: PlatformActivity): TokenStore {
    return SettingsTokenStore(WebLocalStorageSettingsStore())
}