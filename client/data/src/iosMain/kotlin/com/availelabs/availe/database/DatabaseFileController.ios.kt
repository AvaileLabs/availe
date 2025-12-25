package com.availelabs.availe.database

import com.availelabs.availe.platform.PlatformActivity
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

internal class IosDatabaseFileController : IDatabaseFileController {
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

internal actual fun createDatabaseFileController(activity: PlatformActivity): IDatabaseFileController {
    return IosDatabaseFileController()
}