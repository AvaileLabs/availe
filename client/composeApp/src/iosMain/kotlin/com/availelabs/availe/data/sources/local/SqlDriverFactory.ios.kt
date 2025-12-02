package com.availelabs.availe.data.sources.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.db.ClientDatabase

class NativeSqlDriverFactory : SqlDriverFactory {
    override suspend fun createDriver(): SqlDriver {
        return NativeSqliteDriver(ClientDatabase.Schema.synchronous(), "availe.db")
    }
}

actual fun createSqlDriverFactory(activity: PlatformActivity): SqlDriverFactory {
    return NativeSqlDriverFactory()
}