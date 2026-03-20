package com.availelabs.availe.logging

sealed interface LogEnvironment {
    data object Development : LogEnvironment
    data object Production : LogEnvironment
}