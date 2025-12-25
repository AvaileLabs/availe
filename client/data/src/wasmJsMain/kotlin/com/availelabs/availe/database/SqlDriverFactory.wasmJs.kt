package com.availelabs.availe.database

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.availelabs.availe.db.ClientDatabase
import com.availelabs.availe.platform.PlatformActivity
import com.availelabs.availe.sources.local.sqlDelightWorker

class WasmSqlDriverFactory : SqlDriverFactory {
    override suspend fun createDriver(): SqlDriver {
        val driver = WebWorkerDriver(sqlDelightWorker())
        ClientDatabase.Schema.awaitCreate(driver)
        return driver
    }
}

actual fun createSqlDriverFactory(activity: PlatformActivity): SqlDriverFactory {
    return WasmSqlDriverFactory()
}