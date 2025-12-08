package com.availelabs.availe.data.sources.local

import com.availelabs.availe.core.platform.PlatformActivity
import com.availelabs.availe.data.database.IDatabaseFileController

class WebDatabaseFileController : IDatabaseFileController {
    override suspend fun deleteDatabaseFile() {
    }
}

actual fun createDatabaseFileController(activity: PlatformActivity): IDatabaseFileController {
    return WebDatabaseFileController()
}