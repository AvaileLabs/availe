package com.availelabs.availe.data.database

import app.cash.sqldelight.db.SqlDriver
import com.availelabs.availe.core.platform.PlatformActivity

interface SqlDriverFactory {
    suspend fun createDriver(): SqlDriver
}

expect fun createSqlDriverFactory(activity: PlatformActivity): SqlDriverFactory