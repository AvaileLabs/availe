package com.availelabs.availe.data.sources.local

import com.availelabs.availe.core.platform.PlatformActivity

interface IDatabaseFileController {
    suspend fun deleteDatabaseFile()
}

expect fun createDatabaseFileController(activity: PlatformActivity): IDatabaseFileController