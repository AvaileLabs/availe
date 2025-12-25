package com.availelabs.availe.database

import com.availelabs.availe.platform.PlatformActivity

class WebDatabaseFileController : IDatabaseFileController {
    override suspend fun deleteDatabaseFile() {
    }
}

actual fun createDatabaseFileController(activity: PlatformActivity): IDatabaseFileController {
    return WebDatabaseFileController()
}