package com.availelabs.availe.domain.auth

interface SessionManager {
    suspend fun onSessionInvalidated()
}