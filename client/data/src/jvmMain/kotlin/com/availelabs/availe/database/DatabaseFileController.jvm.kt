package com.availelabs.availe.database

import com.availelabs.availe.platform.PlatformActivity
import java.io.File

internal class JvmDatabaseFileController : IDatabaseFileController {
    override suspend fun deleteDatabaseFile() {
        val connectionUrl = ClientDatabaseConfiguration.databaseConnectionUrl.value
        val filePath = connectionUrl.removePrefix("jdbc:sqlite:")
        val databaseFile = File(filePath)
        if (databaseFile.exists()) {
            try {
                databaseFile.delete()
            } catch (_: Exception) {
            }
        }
    }
}

internal actual fun createDatabaseFileController(activity: PlatformActivity): IDatabaseFileController {
    return JvmDatabaseFileController()
}