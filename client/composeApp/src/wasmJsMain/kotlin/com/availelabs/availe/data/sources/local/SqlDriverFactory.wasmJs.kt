package com.availelabs.availe.data.sources.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.db.ClientDatabase
import org.w3c.dom.Worker

class WasmSqlDriverFactory : SqlDriverFactory {
    override suspend fun createDriver(): SqlDriver {
        val worker = createWorker()
        val driver = WebWorkerDriver(worker)
        ClientDatabase.Schema.create(driver).await()
        return driver
    }
}

private fun createWorker(): Worker =
    js("""new Worker(new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url))""")

actual fun createSqlDriverFactory(activity: PlatformActivity): SqlDriverFactory {
    return WasmSqlDriverFactory()
}