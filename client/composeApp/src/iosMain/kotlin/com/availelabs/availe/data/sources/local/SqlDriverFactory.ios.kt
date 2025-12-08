package com.availelabs.availe.data.sources.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.data.database.SqlDriverFactory
import com.availelabs.availe.db.ClientDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class NativeSqlDriverFactory : SqlDriverFactory {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun createDriver(): SqlDriver {
        val databaseName = ClientDatabaseConfiguration.databaseName.value
        return try {
            NativeSqliteDriver(
                schema = ClientDatabase.Schema.synchronous(),
                name = databaseName
            )
        } catch (_: Exception) {
            val fileManager = NSFileManager.defaultManager
            val searchPaths = NSSearchPathForDirectoriesInDomains(
                directory = NSApplicationSupportDirectory,
                domainMask = NSUserDomainMask,
                expandTilde = true
            )
            val applicationSupportDirectory = searchPaths.firstOrNull() as? String
            if (applicationSupportDirectory != null) {
                val databasePath = "$applicationSupportDirectory/$databaseName"
                if (fileManager.fileExistsAtPath(databasePath)) {
                    fileManager.removeItemAtPath(databasePath, null)
                }
            }
            NativeSqliteDriver(
                schema = ClientDatabase.Schema.synchronous(),
                name = databaseName
            )
        }
    }
}

actual fun createSqlDriverFactory(activity: PlatformActivity): SqlDriverFactory {
    return NativeSqlDriverFactory()
}