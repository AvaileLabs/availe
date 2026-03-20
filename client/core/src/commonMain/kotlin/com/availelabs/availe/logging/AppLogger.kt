package com.availelabs.availe.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter

object AppLogger {
    var currentLogRuleset: LogRuleset = LogRuleset(
        defaultSeverity = Severity.Assert,
        categoryRules = emptyMap(),
        classRules = emptyMap()
    )
        private set

    var currentLogWriters: List<LogWriter> = emptyList()
        private set

    fun initialize(logEnvironment: LogEnvironment, logRuleset: LogRuleset) {
        currentLogRuleset = logRuleset
        currentLogWriters = when (logEnvironment) {
            is LogEnvironment.Development -> {
                listOf(platformLogWriter())
            }

            is LogEnvironment.Production -> {
                emptyList()
            }
        }
        Logger.setLogWriters(currentLogWriters)
    }
}