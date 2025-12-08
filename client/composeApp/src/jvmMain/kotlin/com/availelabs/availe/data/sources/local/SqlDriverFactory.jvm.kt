package com.availelabs.availe.data.sources.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.data.database.SqlDriverFactory
import com.availelabs.availe.db.ClientDatabase
import java.io.File

class JvmSqlDriverFactory : SqlDriverFactory {
    override suspend fun createDriver(): SqlDriver {
        val connectionUrl = ClientDatabaseConfiguration.databaseConnectionUrl.value
        return try {
            JdbcSqliteDriver(
                url = connectionUrl,
                schema = ClientDatabase.Schema.synchronous()
            )
        } catch (_: Exception) {
            val filePath = connectionUrl.removePrefix("jdbc:sqlite:")
            val databaseFile = File(filePath)
            if (databaseFile.exists()) {
                databaseFile.delete()
            }
            JdbcSqliteDriver(
                url = connectionUrl,
                schema = ClientDatabase.Schema.synchronous()
            )
        }
    }
}

actual fun createSqlDriverFactory(activity: PlatformActivity): SqlDriverFactory {
    return JvmSqlDriverFactory()
}