package com.availelabs.availe.ui.components

import com.arkivanov.decompose.ComponentContext

interface IErrorComponent {
    val message: String
}

class ErrorComponent(
    componentContext: ComponentContext,
    error: Throwable,
) : IErrorComponent, ComponentContext by componentContext {
    override val message: String = error.message ?: "An unknown error occurred."
}