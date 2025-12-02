package com.availelabs.availe.data.sources.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.db.ClientDatabase
import org.w3c.dom.Worker
import org.w3c.dom.url.URL

class WebSqlDriverFactory : SqlDriverFactory {
    override suspend fun createDriver(): SqlDriver {
        val metaUrl = js("import.meta.url").unsafeCast<String>()
        val workerUrl = URL(ClientDatabaseConfiguration.workerModulePath.value, metaUrl)
        val worker = Worker(workerUrl.toString())
        val driver = WebWorkerDriver(worker)
        ClientDatabase.Schema.create(driver).await()
        return driver
    }
}

actual fun createSqlDriverFactory(activity: PlatformActivity): SqlDriverFactory {
    return WebSqlDriverFactory()
}