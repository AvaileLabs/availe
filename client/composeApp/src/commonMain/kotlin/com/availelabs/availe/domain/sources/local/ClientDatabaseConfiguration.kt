package com.availelabs.availe.data.sources.local

import kotlin.jvm.JvmInline

@JvmInline
value class DatabaseName(val value: String)

@JvmInline
value class DatabaseConnectionUrl(val value: String)

@JvmInline
value class WorkerModulePath(val value: String)

object ClientDatabaseConfiguration {
    val databaseName = DatabaseName("availe.db")
    val databaseConnectionUrl = DatabaseConnectionUrl("jdbc:sqlite:${databaseName.value}")
    val workerModulePath = WorkerModulePath("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js")
}