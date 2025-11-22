package com.availelabs.availe.features.listings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.availelabs.availe.data.auth.IAuthenticationService
import com.availelabs.availe.data.network.IApiClient
import com.availelabs.availe.domain.model.Listing
import kotlinx.serialization.Serializable

interface IMyListingsHostComponent {
    val childStack: Value<ChildStack<*, MyListingsChild>>

    fun showEditor(listing: Listing?)
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

    @OptIn(DelicateDecomposeApi::class)
    override fun showEditor(listing: Listing?) {
        if (listing != null) {
            currentListingToEdit = listing
            navigation.push(Config.Editor(isEdit = true))
        } else {
            currentListingToEdit = null
            navigation.push(Config.Editor(isEdit = false))
        }
    }

    private var currentListingToEdit: Listing? = null

    private fun createChild(config: Config, componentContext: ComponentContext): MyListingsChild =
        when (config) {
            Config.List -> MyListingsChild.List(
                UserListingsComponent(
                    componentContext = componentContext,
                    apiClient = apiClient,
                    authService = authService,
                    onEditorRequest = { listing -> showEditor(listing) }
                )
            )

            is Config.Editor -> MyListingsChild.Editor(
                ListingEditorComponent(
                    componentContext = componentContext,
                    apiClient = apiClient,
                    authService = authService,
                    listingToEdit = if (config.isEdit) currentListingToEdit else null,
                    onFinishedCallback = {
                        currentListingToEdit = null
                        navigation.pop()
                    }
                )
            )
        }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object List : Config

        @Serializable
        data class Editor(val isEdit: Boolean) : Config
    }
}

sealed interface MyListingsChild {
    data class List(val component: IUserListingsComponent) : MyListingsChild
    data class Editor(val component: IListingEditorComponent) : MyListingsChild
}