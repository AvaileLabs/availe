package com.availelabs.availe.database

import com.availelabs.availe.platform.PlatformActivity

internal class WebDatabaseFileController : IDatabaseFileController {
    override suspend fun deleteDatabaseFile() {
    }
}

internal actual fun createDatabaseFileController(activity: PlatformActivity): IDatabaseFileController {
    return WebDatabaseFileController()
}