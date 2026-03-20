package com.availelabs.availe.logging

import co.touchlab.kermit.Severity
import kotlin.reflect.KClass

class LogRuleset(
    val defaultSeverity: Severity,
    val categoryRules: Map<LogCategory, Severity>,
    val classRules: Map<String, Severity>
)

class LogRulesetBuilder {
    private var defaultSeverity: Severity = Severity.Assert
    private val categoryRules = mutableMapOf<LogCategory, Severity>()
    private val classRules = mutableMapOf<String, Severity>()

    fun default(severity: Severity) {
        defaultSeverity = severity
    }

    fun category(logCategory: LogCategory, severity: Severity) {
        categoryRules[logCategory] = severity
    }

    fun override(targetClass: KClass<*>, severity: Severity) {
        val className = targetClass.simpleName
        if (className != null) {
            classRules[className] = severity
        }
    }

    fun build(): LogRuleset {
        return LogRuleset(
            defaultSeverity = defaultSeverity,
            categoryRules = categoryRules.toMap(),
            classRules = classRules.toMap()
        )
    }
}

fun logRuleset(block: LogRulesetBuilder.() -> Unit): LogRuleset {
    val logRulesetBuilder = LogRulesetBuilder()
    logRulesetBuilder.block()
    return logRulesetBuilder.build()
}