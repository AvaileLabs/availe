package com.availelabs.availe.xmpp.session

internal fun detectTerminalHandshakeFailure(rawText: String): String? {
    val text = rawText.trim()
    if (text.isBlank()) return null

    if (text.contains("xmpp-sasl", ignoreCase = true) && text.contains("<failure", ignoreCase = true)) {
        val condition = listOf(
            "not-authorized",
            "invalid-authzid",
            "account-disabled",
            "credentials-expired",
            "encryption-required",
            "incorrect-encoding",
            "invalid-mechanism",
            "malformed-request",
            "mechanism-too-weak",
            "temporary-auth-failure"
        ).firstOrNull { conditionName ->
            text.contains("<$conditionName", ignoreCase = true)
        }

        val failureText = Regex("(?s)<text[^>]*>(.*?)</text>", setOf(RegexOption.IGNORE_CASE))
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        val details = listOfNotNull(condition, failureText).joinToString(": ")
        return if (details.isBlank()) {
            "SASL Authentication Failed"
        } else {
            "SASL Authentication Failed: $details"
        }
    }

    if (text.contains("xmpp-sasl", ignoreCase = true) && text.contains("<challenge", ignoreCase = true)) {
        return "SASL challenge received but is not supported by this client: $text"
    }

    if (text.contains("<stream:error", ignoreCase = true)) {
        return "Stream error: $text"
    }

    return null
}