package com.availelabs.availe.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.availelabs.availe.db.ClientDatabase
import com.availelabs.availe.platform.PlatformActivity
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

internal class NativeSqlDriverFactory : SqlDriverFactory {
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

internal actual fun createSqlDriverFactory(activity: PlatformActivity): SqlDriverFactory {
    return NativeSqlDriverFactory()
}