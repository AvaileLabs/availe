package com.availelabs.availe.database

import kotlin.jvm.JvmInline

@JvmInline
internal value class DatabaseName(val value: String)

@JvmInline
internal value class DatabaseConnectionUrl(val value: String)

@JvmInline
internal value class WorkerModulePath(val value: String)

internal object ClientDatabaseConfiguration {
    val databaseName = DatabaseName("availe.db")
    val databaseConnectionUrl =
        DatabaseConnectionUrl("jdbc:sqlite:${databaseName.value}")
    val workerModulePath =
        WorkerModulePath("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js")
}