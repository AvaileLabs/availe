package com.availelabs.availe.data.sources.local

import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.data.database.IDatabaseFileController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class IosDatabaseFileController : IDatabaseFileController {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun deleteDatabaseFile() {
        val fileManager = NSFileManager.defaultManager
        val searchPaths = NSSearchPathForDirectoriesInDomains(
            directory = NSApplicationSupportDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true
        )
        val applicationSupportDirectory = searchPaths.firstOrNull() as? String
        if (applicationSupportDirectory != null) {
            val databaseName = ClientDatabaseConfiguration.databaseName.value
            val databasePath = "$applicationSupportDirectory/$databaseName"
            if (fileManager.fileExistsAtPath(databasePath)) {
                try {
                    fileManager.removeItemAtPath(databasePath, null)
                } catch (_: Exception) {
                }
            }
        }
    }
}

actual fun createDatabaseFileController(activity: PlatformActivity): IDatabaseFileController {
    return IosDatabaseFileController()
}