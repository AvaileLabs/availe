package com.availelabs.availe.auth

sealed interface IdentityResolutionResult {
    data class Success(val authenticatedUser: AuthenticatedUser) : IdentityResolutionResult
    data object MissingHeader : IdentityResolutionResult
    data object InvalidToken : IdentityResolutionResult
}