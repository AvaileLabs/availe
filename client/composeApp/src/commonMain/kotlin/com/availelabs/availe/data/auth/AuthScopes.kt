package com.availelabs.availe.data.auth

enum class AuthScope(val value: String) {
    OPEN_ID("openid"),
    EMAIL("email"),
    OFFLINE_ACCESS("offline_access");
}

fun Set<AuthScope>.toScopeString(): String = joinToString(" ") { it.value }