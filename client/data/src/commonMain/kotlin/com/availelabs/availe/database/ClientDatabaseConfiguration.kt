package com.availelabs.availe.database

import kotlin.jvm.JvmInline

@JvmInline
value class DatabaseName(val value: String)

@JvmInline
value class DatabaseConnectionUrl(val value: String)

@JvmInline
value class WorkerModulePath(val value: String)

object ClientDatabaseConfiguration {
    val databaseName = _root_ide_package_.com.availelabs.availe.database.DatabaseName("availe.db")
    val databaseConnectionUrl =
        _root_ide_package_.com.availelabs.availe.database.DatabaseConnectionUrl("jdbc:sqlite:${databaseName.value}")
    val workerModulePath =
        _root_ide_package_.com.availelabs.availe.database.WorkerModulePath("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js")
}