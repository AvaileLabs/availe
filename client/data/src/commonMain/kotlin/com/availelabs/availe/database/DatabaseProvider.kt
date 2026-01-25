package com.availelabs.availe.database

import app.cash.sqldelight.db.SqlDriver
import com.availelabs.availe.db.ClientDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

internal interface IDatabaseProvider {
    suspend fun getDatabase(): ClientDatabase
    suspend fun closeAndClearDatabase()
}

internal class DatabaseProvider(
    private val sqlDriverFactory: com.availelabs.availe.database.SqlDriverFactory,
    private val json: Json
) : com.availelabs.availe.database.IDatabaseProvider {

    private var sqlDriver: SqlDriver? = null
    private var clientDatabase: ClientDatabase? = null
    private val mutex = Mutex()

    override suspend fun getDatabase(): ClientDatabase = mutex.withLock {
        if (clientDatabase != null) {
            return clientDatabase!!
        }

        val driver = sqlDriverFactory.createDriver()
        val database =
            ClientDatabaseFactory.createClientDatabase(driver, json)

        sqlDriver = driver
        clientDatabase = database

        return database
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
}