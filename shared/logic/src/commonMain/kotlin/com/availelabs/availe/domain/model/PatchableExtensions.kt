package com.availelabs.availe.domain.model

import io.availe.models.Patchable

fun <T> Patchable<T>.valueOrExisting(current: T): T {
    return when (this) {
        is Patchable.Set -> this.value
        is Patchable.Unchanged -> current
    }
}