package com.availelabs.availe.decompose

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

fun <T : Any> StateFlow<T>.asValue(
    lifecycle: Lifecycle,
    context: CoroutineContext = Dispatchers.Main.immediate,
): Value<T> {
    val scope = CoroutineScope(context)
    val mutableValue = MutableValue(value)

    scope.launch {
        collect { mutableValue.value = it }
    }

    lifecycle.doOnDestroy {
        scope.cancel()
    }

    return mutableValue
}