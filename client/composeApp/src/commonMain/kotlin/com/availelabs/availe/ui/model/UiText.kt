package com.availelabs.availe.ui.model

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

sealed interface UiText {
    data class VerbatimString(val value: String) : UiText
    data class ResourceString(
        val resource: StringResource,
        val formatArguments: List<Any> = emptyList()
    ) : UiText
}

@Composable
fun UiText.asString(): String {
    return when (this) {
        is UiText.VerbatimString -> this.value
        is UiText.ResourceString -> stringResource(this.resource, *this.formatArguments.toTypedArray())
    }
}