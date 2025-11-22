@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.listings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnResume
import com.availelabs.availe.data.auth.IAuthenticationService
import com.availelabs.availe.data.network.IApiClient
import com.availelabs.availe.domain.model.Listing
import com.availelabs.ui.utils.ContentLoader
import com.availelabs.ui.utils.ContentState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

interface IUserListingsComponent {
    val state: Value<ContentState<List<Listing>>>
    fun onCreateClicked()
    fun onEditClicked(listing: Listing)
    fun onDeleteClicked(listing: Listing)
    fun onRefresh()
}

class UserListingsComponent(
    componentContext: ComponentContext,
    private val apiClient: IApiClient,
    private val authService: IAuthenticationService,
    private val onEditorRequest: (Listing?) -> Unit
) : IUserListingsComponent, ComponentContext by componentContext {

    private val scope = componentContext.coroutineScope(Dispatchers.Main.immediate)

    private val listingsLoader = ContentLoader(
        scope = scope,
        fetcher = ::fetchUserListings
    )

    override val state: Value<ContentState<List<Listing>>> = listingsLoader.state

    init {
        listingsLoader.load()

        lifecycle.doOnResume {
            listingsLoader.refresh()
        }
    }

    override fun onCreateClicked() {
        onEditorRequest(null)
    }

    override fun onEditClicked(listing: Listing) {
        onEditorRequest(listing)
    }

    override fun onDeleteClicked(listing: Listing) {
        scope.launch {
            try {
                val user = authService.getCurrentUserProfile() ?: return@launch
                apiClient.deleteListing(listing.id, user.id)
                listingsLoader.refresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onRefresh() {
        listingsLoader.refresh()
    }

    private suspend fun fetchUserListings(): List<Listing> {
        val user = authService.getCurrentUserProfile() ?: return emptyList()
        val allListings = apiClient.getListings()
        return allListings.filter { it.publisherId == user.id }
    }
}