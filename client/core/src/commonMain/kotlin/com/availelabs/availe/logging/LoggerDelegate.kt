package com.availelabs.availe.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import kotlin.properties.ReadOnlyProperty

inline fun <reified TargetClass : Any> TargetClass.logger(logCategory: LogCategory): ReadOnlyProperty<TargetClass, Logger> {
    val loggerTag = TargetClass::class.simpleName ?: "UnknownClass"

    val logRuleset = AppLogger.currentLogRuleset
    val resolvedSeverity = logRuleset.classRules[loggerTag]
        ?: logRuleset.categoryRules[logCategory]
        ?: logRuleset.defaultSeverity

    val kermitLogger = Logger(
        config = StaticConfig(
            minSeverity = resolvedSeverity,
            logWriterList = AppLogger.currentLogWriters
        ),
        tag = loggerTag
    )

    return ReadOnlyProperty { _, _ -> kermitLogger }
}