package com.availelabs.availe.xmpp.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class XmppJid(val value: String)

@Serializable
@JvmInline
value class StanzaIdentifier(val value: String)

@Serializable
@JvmInline
value class LanguageTag(val value: String)

@Serializable
@JvmInline
value class SaslMechanism(val value: String)

@Serializable
@JvmInline
value class MessageBody(val value: String)

@Serializable
@JvmInline
value class ResourceName(val value: String)