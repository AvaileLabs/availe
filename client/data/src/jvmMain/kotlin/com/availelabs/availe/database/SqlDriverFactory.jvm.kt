package com.availelabs.availe.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.availelabs.availe.db.ClientDatabase
import com.availelabs.availe.platform.PlatformActivity
import java.io.File

internal class JvmSqlDriverFactory : SqlDriverFactory {
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

internal actual fun createSqlDriverFactory(activity: PlatformActivity): SqlDriverFactory {
    return JvmSqlDriverFactory()
}