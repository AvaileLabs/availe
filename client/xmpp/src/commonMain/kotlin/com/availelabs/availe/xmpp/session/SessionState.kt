package com.availelabs.availe.xmpp.session

sealed interface SessionState {
    data object Disconnected : SessionState
    data object Connecting : SessionState
    data object Negotiating : SessionState
    data object Authenticating : SessionState
    data object Binding : SessionState
    data object Active : SessionState
    data class Failed(val reason: String, val exception: Throwable? = null) : SessionState
}