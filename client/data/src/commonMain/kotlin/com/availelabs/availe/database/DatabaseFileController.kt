package com.availelabs.availe.database

import com.availelabs.availe.platform.PlatformActivity

internal interface IDatabaseFileController {
    suspend fun deleteDatabaseFile()
}

internal expect fun createDatabaseFileController(activity: PlatformActivity): IDatabaseFileController