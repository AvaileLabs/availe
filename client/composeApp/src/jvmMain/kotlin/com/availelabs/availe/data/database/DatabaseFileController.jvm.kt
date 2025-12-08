package com.availelabs.availe.data.database

import com.availelabs.availe.core.platform.PlatformActivity
import java.io.File

class JvmDatabaseFileController : IDatabaseFileController {
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

actual fun createDatabaseFileController(activity: PlatformActivity): IDatabaseFileController {
    return JvmDatabaseFileController()
}