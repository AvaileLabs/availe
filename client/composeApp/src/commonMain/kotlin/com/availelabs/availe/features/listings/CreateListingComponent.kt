package com.availelabs.availe.features.listings

import com.arkivanov.decompose.ComponentContext
import com.availelabs.availe.data.auth.IAuthenticationService
import com.availelabs.availe.data.network.IApiClient

interface ICreateListingComponent {
    fun onFinished()
}

class CreateListingComponent(
    componentContext: ComponentContext,
    private val apiClient: IApiClient,
    private val authService: IAuthenticationService,
    private val onFinishedCallback: () -> Unit
) : ICreateListingComponent, ComponentContext by componentContext {

    override fun onFinished() {
        onFinishedCallback()
    }
}