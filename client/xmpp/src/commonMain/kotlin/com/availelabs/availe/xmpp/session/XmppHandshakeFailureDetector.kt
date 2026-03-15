package com.availelabs.availe.xmpp.session

internal fun detectTerminalHandshakeFailure(rawStanza: String): String? {
    val normalizedStanza = rawStanza.lowercase()

    return when {
        "<failure" in normalizedStanza && "xmpp-sasl" in normalizedStanza -> {
            val condition = when {
                "<not-authorized" in normalizedStanza -> "not-authorized"
                "<temporary-auth-failure" in normalizedStanza -> "temporary-auth-failure"
                "<invalid-authzid" in normalizedStanza -> "invalid-authzid"
                else -> "unknown"
            }
            "SASL Authentication Failed: $condition"
        }

        "<challenge" in normalizedStanza && "xmpp-sasl" in normalizedStanza -> {
            "Unexpected SASL challenge"
        }

        "<stream:error" in normalizedStanza -> {
            "Stream error detected during handshake"
        }

        else -> null
    }
}