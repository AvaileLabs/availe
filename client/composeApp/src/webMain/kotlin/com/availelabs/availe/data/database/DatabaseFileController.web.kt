package com.availelabs.availe.data.database

import com.availelabs.availe.core.platform.PlatformActivity

class WebDatabaseFileController : IDatabaseFileController {
    override suspend fun deleteDatabaseFile() {
    }
}

actual fun createDatabaseFileController(activity: PlatformActivity): IDatabaseFileController {
    return WebDatabaseFileController()
}