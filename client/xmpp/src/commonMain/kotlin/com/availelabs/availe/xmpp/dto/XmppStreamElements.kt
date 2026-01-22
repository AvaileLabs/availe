package com.availelabs.availe.xmpp.dto

import com.availelabs.availe.xmpp.model.*
import com.availelabs.availe.xmpp.xml.XmppNamespaces
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlPolyChildren
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
sealed interface IXmppStreamElement

// --- Transport & Handshake Elements ---

@Serializable
@XmlSerialName(value = "open", namespace = XmppNamespaces.Framing, prefix = "")
data class XmppStreamOpen(
    @XmlSerialName(value = "to", namespace = "")
    val to: String,

    @XmlSerialName(value = "version", namespace = "")
    val version: String = "1.0",

    @XmlSerialName(value = "xml:lang", namespace = "")
    val language: LanguageTag? = null,

    @XmlSerialName(value = "id", namespace = "")
    val identifier: String? = null
) : IXmppStreamElement

@Serializable
@XmlSerialName(value = "features", namespace = XmppNamespaces.Streams, prefix = "stream")
data class XmppStreamFeatures(
    @XmlElement(true)
    @XmlSerialName(value = "mechanisms", namespace = XmppNamespaces.Sasl, prefix = "")
    val mechanisms: XmppSaslMechanisms? = null,

    @XmlElement(true)
    @XmlSerialName(value = "bind", namespace = XmppNamespaces.Bind, prefix = "")
    val bind: XmppBindFeature? = null
) : IXmppStreamElement

@Serializable
@XmlSerialName(value = "mechanisms", namespace = XmppNamespaces.Sasl, prefix = "")
data class XmppSaslMechanisms(
    @XmlElement(true)
    @XmlSerialName(value = "mechanism", namespace = XmppNamespaces.Sasl, prefix = "")
    val mechanism: List<SaslMechanism>
)

@Serializable
@XmlSerialName(value = "bind", namespace = XmppNamespaces.Bind, prefix = "")
data class XmppBindFeature(
    @XmlElement(false)
    val required: String? = null
)

@Serializable
@XmlSerialName(value = "auth", namespace = XmppNamespaces.Sasl, prefix = "")
data class XmppSaslAuth(
    @XmlSerialName(value = "mechanism", namespace = "")
    val mechanism: SaslMechanism,

    @XmlValue
    val content: String
) : IXmppStreamElement

@Serializable
@XmlSerialName(value = "success", namespace = XmppNamespaces.Sasl, prefix = "")
data class XmppSaslSuccess(
    @XmlValue
    val content: String? = null
) : IXmppStreamElement

@Serializable
@XmlSerialName(value = "failure", namespace = XmppNamespaces.Sasl, prefix = "")
data class XmppSaslFailure(
    @XmlElement(true)
    @XmlSerialName(value = "not-authorized", namespace = XmppNamespaces.Sasl, prefix = "")
    val notAuthorized: String? = null
) : IXmppStreamElement

// --- Active Session Stanzas ---

@Serializable
@XmlSerialName(value = "message", namespace = XmppNamespaces.JabberClient, prefix = "")
data class XmppMessageStanza(
    @XmlSerialName(value = "to", namespace = "")
    val recipientJid: XmppJid? = null,

    @XmlSerialName(value = "from", namespace = "")
    val senderJid: XmppJid? = null,

    @XmlSerialName(value = "id", namespace = "")
    val identifier: StanzaIdentifier? = null,

    @XmlSerialName(value = "type", namespace = "")
    val type: String = "chat",

    @XmlElement(true)
    @XmlSerialName(value = "body", namespace = XmppNamespaces.JabberClient, prefix = "")
    val body: MessageBody? = null
) : IXmppStreamElement

@Serializable
@XmlSerialName(value = "presence", namespace = XmppNamespaces.JabberClient, prefix = "")
data class XmppPresenceStanza(
    @XmlSerialName(value = "to", namespace = "")
    val recipientJid: XmppJid? = null,

    @XmlSerialName(value = "from", namespace = "")
    val senderJid: XmppJid? = null,

    @XmlSerialName(value = "type", namespace = "")
    val type: String? = null
) : IXmppStreamElement

@Serializable
@XmlSerialName(value = "iq", namespace = XmppNamespaces.JabberClient, prefix = "")
data class XmppIqStanza(
    @XmlSerialName(value = "id", namespace = "")
    val identifier: StanzaIdentifier,

    @XmlSerialName(value = "type", namespace = "")
    val type: String,

    @XmlSerialName(value = "to", namespace = "")
    val recipientJid: XmppJid? = null,

    @XmlSerialName(value = "from", namespace = "")
    val senderJid: XmppJid? = null,

    @XmlPolyChildren(["bind"])
    val payload: IIqPayload? = null
) : IXmppStreamElement