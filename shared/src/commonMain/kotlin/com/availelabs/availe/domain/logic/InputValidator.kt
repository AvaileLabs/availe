package com.availelabs.availe.domain.logic

object InputValidator {
    const val MAX_TITLE_LENGTH = 100

    fun isValidPrice(input: String): Boolean {
        if (input.isEmpty()) return true
        return input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1
    }

    fun isValidTitle(input: String): Boolean {
        return input.isNotBlank() && input.length <= MAX_TITLE_LENGTH
    }

    fun isValidUrl(input: String): Boolean {
        return input.isNotBlank()
    }
}