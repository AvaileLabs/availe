package com.availelabs.availe.data.sources.local

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.db.ClientDatabase

class WebSqlDriverFactory : SqlDriverFactory {
    override suspend fun createDriver(): SqlDriver {
        val driver = WebWorkerDriver(sqlDelightWorker())
        ClientDatabase.Schema.awaitCreate(driver)
        return driver
    }
}

actual fun createSqlDriverFactory(activity: PlatformActivity): SqlDriverFactory {
    return WebSqlDriverFactory()
}