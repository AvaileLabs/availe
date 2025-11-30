package com.availelabs.availe.data.auth

import com.availelabs.availe.configuration.SharedAuthenticationConstants

enum class AuthScope(val value: String) {
    OPEN_ID(SharedAuthenticationConstants.Scopes.OPEN_ID),
    EMAIL(SharedAuthenticationConstants.Scopes.EMAIL),
    OFFLINE_ACCESS(SharedAuthenticationConstants.Scopes.OFFLINE_ACCESS);
}

fun Set<AuthScope>.toScopeString(): String = joinToString(" ") { it.value }