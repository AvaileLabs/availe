package com.availelabs.availe.preferences

import com.russhwolf.settings.Settings

internal interface IDatabasePreferences {
    suspend fun getInstalledSchemaHash(): String?
    suspend fun updateInstalledSchemaHash(hash: String)
    suspend fun clear()
}

internal class DatabasePreferences(
    private val settings: Settings
) : IDatabasePreferences {

    override suspend fun getInstalledSchemaHash(): String? {
        return if (settings.hasKey(KEY_INSTALLED_SCHEMA_HASH)) {
            settings.getString(KEY_INSTALLED_SCHEMA_HASH, "")
        } else {
            null
        }
    }

    override suspend fun updateInstalledSchemaHash(hash: String) {
        settings.putString(KEY_INSTALLED_SCHEMA_HASH, hash)
    }

    override suspend fun clear() {
        settings.remove(KEY_INSTALLED_SCHEMA_HASH)
    }

    companion object {
        private const val KEY_INSTALLED_SCHEMA_HASH = "key_installed_database_schema_hash"
    }
}