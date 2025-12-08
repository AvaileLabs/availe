package com.availelabs.availe.data.database

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.db.ClientDatabase

class AndroidSqlDriverFactory(private val context: Context) : SqlDriverFactory {
    override suspend fun createDriver(): SqlDriver {
        val databaseName = ClientDatabaseConfiguration.databaseName.value
        return try {
            AndroidSqliteDriver(
                schema = ClientDatabase.Schema.synchronous(),
                context = context,
                name = databaseName
            )
        } catch (_: Exception) {
            context.deleteDatabase(databaseName)
            AndroidSqliteDriver(
                schema = ClientDatabase.Schema.synchronous(),
                context = context,
                name = databaseName
            )
        }
    }
}

actual fun createSqlDriverFactory(activity: PlatformActivity): SqlDriverFactory {
    return AndroidSqlDriverFactory(activity)
}