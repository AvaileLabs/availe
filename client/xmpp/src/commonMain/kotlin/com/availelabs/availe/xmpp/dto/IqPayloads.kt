package com.availelabs.availe.xmpp.dto

import com.availelabs.availe.xmpp.model.ResourceName
import com.availelabs.availe.xmpp.model.XmppJid
import com.availelabs.availe.xmpp.xml.XmppNamespaces
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
sealed interface IIqPayload

@Serializable
@XmlSerialName(value = "bind", namespace = XmppNamespaces.Bind, prefix = "")
data class XmppBindPayload(
    @XmlSerialName(value = "resource", namespace = XmppNamespaces.Bind, prefix = "")
    val resource: ResourceName? = null,

    @XmlSerialName(value = "jid", namespace = XmppNamespaces.Bind, prefix = "")
    val jid: XmppJid? = null
) : IIqPayload