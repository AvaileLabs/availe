package com.availelabs.availe.database

import android.content.Context
import com.availelabs.availe.platform.PlatformActivity

class AndroidDatabaseFileController(private val context: Context) :
    IDatabaseFileController {
    override suspend fun deleteDatabaseFile() {
        val databaseName =
            ClientDatabaseConfiguration.databaseName.value
        context.deleteDatabase(databaseName)
    }
}

actual fun createDatabaseFileController(activity: PlatformActivity): IDatabaseFileController {
    return AndroidDatabaseFileController(activity)
}