package com.availelabs.availe.database

import com.availelabs.availe.platform.PlatformActivity

interface IDatabaseFileController {
    suspend fun deleteDatabaseFile()
}

expect fun createDatabaseFileController(activity: PlatformActivity): com.availelabs.availe.database.IDatabaseFileController