package com.availelabs.availe.data.sources.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.db.ClientDatabase

class JvmSqlDriverFactory : SqlDriverFactory {
    override suspend fun createDriver(): SqlDriver {
        return JdbcSqliteDriver(
            url = ClientDatabaseConfiguration.databaseConnectionUrl.value,
            schema = ClientDatabase.Schema.synchronous()
        )
    }
}

actual fun createSqlDriverFactory(activity: PlatformActivity): SqlDriverFactory {
    return JvmSqlDriverFactory()
}