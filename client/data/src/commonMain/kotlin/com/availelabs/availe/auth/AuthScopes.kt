package com.availelabs.availe.auth

import com.availelabs.availe.configuration.SharedAuthenticationConstants

internal enum class AuthScope(val value: String) {
    OPEN_ID(SharedAuthenticationConstants.Scopes.OPEN_ID),
    EMAIL(SharedAuthenticationConstants.Scopes.EMAIL),
    OFFLINE_ACCESS(SharedAuthenticationConstants.Scopes.OFFLINE_ACCESS);
}

internal fun Set<AuthScope>.toScopeString(): String = joinToString(" ") { it.value }