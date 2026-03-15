package com.availelabs.availe.xmpp.session

internal fun splitTopLevelXmlElements(rawFrame: String): List<String> {
    val xmppStreamBuffer = XmppStreamBuffer()
    xmppStreamBuffer.append(rawFrame)

    return buildList {
        while (true) {
            val nextStanza = xmppStreamBuffer.extractNextStanza() ?: break
            add(nextStanza)
        }
    }
}