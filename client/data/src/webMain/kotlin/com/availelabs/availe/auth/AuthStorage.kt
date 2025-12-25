package com.availelabs.availe.auth

import kotlinx.browser.localStorage

object AuthStorage {
    private const val KEY_VERIFIER = "auth_code_verifier"
    private const val KEY_STATE = "auth_state"
    private const val KEY_NONCE = "auth_nonce"
    private const val KEY_REDIRECT_URI = "auth_redirect_uri"

    fun saveAuthParams(verifier: String, state: String, nonce: String?, redirectUri: String?) {
        localStorage.setItem(KEY_VERIFIER, verifier)
        localStorage.setItem(KEY_STATE, state)
        if (nonce != null) localStorage.setItem(KEY_NONCE, nonce)
        if (redirectUri != null) localStorage.setItem(KEY_REDIRECT_URI, redirectUri)
    }

    fun getVerifier(): String? = localStorage.getItem(KEY_VERIFIER)
    fun getState(): String? = localStorage.getItem(KEY_STATE)
    fun getNonce(): String? = localStorage.getItem(KEY_NONCE)
    fun getRedirectUri(): String? = localStorage.getItem(KEY_REDIRECT_URI)

    fun clear() {
        localStorage.removeItem(KEY_VERIFIER)
        localStorage.removeItem(KEY_STATE)
        localStorage.removeItem(KEY_NONCE)
        localStorage.removeItem(KEY_REDIRECT_URI)
    }
}