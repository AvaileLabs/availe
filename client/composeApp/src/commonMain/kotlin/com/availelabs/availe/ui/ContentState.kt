package com.availelabs.ui.utils

sealed interface ContentState<out T> {
    data object Idle : ContentState<Nothing>
    data object Loading : ContentState<Nothing>
    data class Content<T>(val data: T) : ContentState<T>
    data class Error(val message: String) : ContentState<Nothing>
}