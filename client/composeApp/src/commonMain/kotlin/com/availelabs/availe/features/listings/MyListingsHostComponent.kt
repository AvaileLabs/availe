package com.availelabs.availe.features.listings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.availelabs.availe.data.auth.IAuthenticationService
import com.availelabs.availe.data.network.IApiClient
import kotlinx.serialization.Serializable

interface IMyListingsHostComponent {
    val childStack: Value<ChildStack<*, MyListingsChild>>

    fun showCreateListing()
}

class MyListingsHostComponent(
    componentContext: ComponentContext,
    private val apiClient: IApiClient,
    private val authService: IAuthenticationService
) : IMyListingsHostComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    override val childStack: Value<ChildStack<*, MyListingsChild>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.List,
        handleBackButton = true,
        childFactory = ::createChild
    )

    override fun showCreateListing() {
        navigation.bringToFront(Config.Create)
    }

    private fun createChild(config: Config, componentContext: ComponentContext): MyListingsChild =
        when (config) {
            Config.List -> MyListingsChild.List(
                UserListingsComponent(
                    componentContext = componentContext,
                    apiClient = apiClient,
                    authService = authService,
                    onCreateClickedCallback = { navigation.push(Config.Create) }
                )
            )

            Config.Create -> MyListingsChild.Create(
                CreateListingComponent(
                    componentContext = componentContext,
                    apiClient = apiClient,
                    authService = authService,
                    onFinishedCallback = { navigation.pop() }
                )
            )
        }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object List : Config

        @Serializable
        data object Create : Config
    }
}

sealed interface MyListingsChild {
    data class List(val component: IUserListingsComponent) : MyListingsChild
    data class Create(val component: ICreateListingComponent) : MyListingsChild
}