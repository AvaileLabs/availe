package com.availelabs.availe.database

import app.cash.sqldelight.db.SqlDriver
import com.availelabs.availe.db.ClientDatabase
import com.availelabs.availe.preferences.IDatabasePreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

internal interface IDatabaseProvider {
    suspend fun getDatabase(): ClientDatabase
    suspend fun closeAndClearDatabase()
}

internal class DatabaseProvider(
    private val sqlDriverFactory: SqlDriverFactory,
    private val json: Json,
    private val databasePreferences: IDatabasePreferences,
    private val databaseFileController: IDatabaseFileController
) : IDatabaseProvider {

    private var sqlDriver: SqlDriver? = null
    private var clientDatabase: ClientDatabase? = null
    private val mutex = Mutex()

    override suspend fun getDatabase(): ClientDatabase = mutex.withLock {
        if (clientDatabase != null) {
            return clientDatabase!!
        }

        validateSchemaAndWipeIfNeeded()

        try {
            return initializeDatabaseAndStoreReference()
        } catch (_: Exception) {
            recoverFromCorruption()
            return initializeDatabaseAndStoreReference()
        }
    }

    override suspend fun closeAndClearDatabase() {
        mutex.withLock {
            try {
                sqlDriver?.close()
            } catch (_: Exception) {
            } finally {
                sqlDriver = null
                clientDatabase = null
            }
        }
    }

    private suspend fun initializeDatabaseAndStoreReference(): ClientDatabase {
        val driver = sqlDriverFactory.createDriver()
        val database = ClientDatabaseFactory.createClientDatabase(driver, json)

        sqlDriver = driver
        clientDatabase = database

        return database
    }

    private suspend fun validateSchemaAndWipeIfNeeded() {
        val currentSchemaHash = DatabaseBuildConfig.SCHEMA_HASH
        val installedSchemaHash = databasePreferences.getInstalledSchemaHash()

        if (installedSchemaHash.isNullOrEmpty()) {
            try {
                databaseFileController.deleteDatabaseFile()
            } catch (_: Exception) {
            }
            databasePreferences.updateInstalledSchemaHash(currentSchemaHash)
        } else if (installedSchemaHash != currentSchemaHash) {
            try {
                databaseFileController.deleteDatabaseFile()
            } catch (_: Exception) {
            }
            databasePreferences.updateInstalledSchemaHash(currentSchemaHash)
        }
    }

    private suspend fun recoverFromCorruption() {
        try {
            sqlDriver?.close()
        } catch (_: Exception) {
        } finally {
            sqlDriver = null
            clientDatabase = null
        }

        try {
            databaseFileController.deleteDatabaseFile()
        } catch (_: Exception) {
        }

        databasePreferences.clear()
    }
}