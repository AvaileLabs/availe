package com.availelabs.availe.dto

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@XmlSerialName(value = "open", namespace = "urn:ietf:params:xml:ns:xmpp-framing", prefix = "")
data class XmppOpenStanza(
    @XmlSerialName(value = "to")
    val to: String,
    @XmlSerialName(value = "version")
    val version: String
)

@Serializable
@XmlSerialName(value = "auth", namespace = "urn:ietf:params:xml:ns:xmpp-sasl", prefix = "")
data class XmppAuthStanza(
    @XmlSerialName(value = "mechanism")
    val mechanism: String,
    @XmlValue
    val content: String
)

@Serializable
@XmlSerialName(value = "bind", namespace = "urn:ietf:params:xml:ns:xmpp-bind", prefix = "")
data class XmppBindStanza(
    val resource: String = "availe-app"
)

@Serializable
@XmlSerialName(value = "iq", namespace = "jabber:client", prefix = "")
data class XmppIqStanza(
    @XmlSerialName(value = "type")
    val type: String,
    @XmlSerialName(value = "id")
    val id: String,
    @XmlElement
    val bind: com.availelabs.availe.dto.XmppBindStanza? = null
)

@Serializable
@XmlSerialName(value = "presence", namespace = "jabber:client", prefix = "")
data class XmppPresenceStanza(
    @XmlSerialName(value = "type")
    val type: String? = null
)

@Serializable
@XmlSerialName(value = "message", namespace = "jabber:client", prefix = "")
data class XmppMessageStanza(
    @XmlSerialName(value = "to")
    val to: String? = null,
    @XmlSerialName(value = "from")
    val from: String? = null,
    @XmlSerialName(value = "type")
    val type: String = "chat",
    @XmlElement
    val body: String? = null
)