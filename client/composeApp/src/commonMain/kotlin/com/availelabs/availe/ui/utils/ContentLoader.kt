package com.availelabs.availe.ui.utils

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ContentLoader<T>(
    private val scope: CoroutineScope,
    private val fetcher: suspend () -> T
) {
    private val _state = MutableValue<ContentState<T>>(ContentState.Idle)
    val state: Value<ContentState<T>> = _state

    fun load() {
        if (_state.value is ContentState.Idle || _state.value is ContentState.Error) {
            performFetch()
        }
    }

    fun refresh() {
        performFetch()
    }

    private fun performFetch() {
        _state.value = ContentState.Loading
        scope.launch {
            try {
                val data = fetcher()
                _state.value = ContentState.Content(data)
            } catch (e: Exception) {
                _state.value = ContentState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}