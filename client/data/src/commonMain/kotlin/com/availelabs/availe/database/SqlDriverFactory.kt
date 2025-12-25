package com.availelabs.availe.database

import app.cash.sqldelight.db.SqlDriver
import com.availelabs.availe.platform.PlatformActivity

internal interface SqlDriverFactory {
    suspend fun createDriver(): SqlDriver
}

internal expect fun createSqlDriverFactory(activity: PlatformActivity): com.availelabs.availe.database.SqlDriverFactory