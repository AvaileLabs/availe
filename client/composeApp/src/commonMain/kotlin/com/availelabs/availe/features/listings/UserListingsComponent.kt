package com.availelabs.availe.features.listings

import com.arkivanov.decompose.ComponentContext
import com.availelabs.availe.data.auth.IAuthenticationService
import com.availelabs.availe.data.network.IApiClient

interface IUserListingsComponent {
    fun onCreateClicked()
}

class UserListingsComponent(
    componentContext: ComponentContext,
    private val apiClient: IApiClient,
    private val authService: IAuthenticationService,
    private val onCreateClickedCallback: () -> Unit
) : IUserListingsComponent, ComponentContext by componentContext {

    override fun onCreateClicked() {
        onCreateClickedCallback()
    }
}