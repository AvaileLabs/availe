package com.availelabs.availe.data.sources.local

import android.content.Context
import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.data.database.IDatabaseFileController

class AndroidDatabaseFileController(private val context: Context) : IDatabaseFileController {
    override suspend fun deleteDatabaseFile() {
        val databaseName = ClientDatabaseConfiguration.databaseName.value
        context.deleteDatabase(databaseName)
    }
}

actual fun createDatabaseFileController(activity: PlatformActivity): IDatabaseFileController {
    return AndroidDatabaseFileController(activity)
}