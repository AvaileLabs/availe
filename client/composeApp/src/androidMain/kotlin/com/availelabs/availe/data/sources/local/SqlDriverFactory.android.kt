package com.availelabs.availe.data.sources.local

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.db.ClientDatabase

class AndroidSqlDriverFactory(private val context: Context) : SqlDriverFactory {
    override suspend fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = ClientDatabase.Schema.synchronous(),
            context = context,
            name = ClientDatabaseConfiguration.databaseName.value
        )
    }
}

actual fun createSqlDriverFactory(activity: PlatformActivity): SqlDriverFactory {
    return AndroidSqlDriverFactory(activity)
}