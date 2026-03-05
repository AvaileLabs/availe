package com.availelabs.availe.appshell.router

import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.availelabs.availe.appshell.component.AppShellChildComponent
import com.availelabs.availe.appshell.model.AppShellConfiguration
import com.availelabs.availe.chat.root.IChatNavigator
import com.availelabs.availe.core.platform.IInAppBrowserLauncher
import com.availelabs.availe.dashboard.IDashboardComponent
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.home.IHomeNavigator
import com.availelabs.availe.listingdetails.IListingDetailsNavigator
import com.availelabs.availe.listingeditor.IListingEditorNavigator
import com.availelabs.availe.login.ILoginNavigator
import com.availelabs.availe.profile.IProfileNavigator
import com.availelabs.availe.settings.ISettingsNavigator
import com.availelabs.availe.ui.model.LayoutMode

actual class AppShellRouter actual constructor(
    private val stackNavigation: StackNavigation<AppShellConfiguration>,
    private val overlayNavigation: SlotNavigation<AppShellConfiguration.ChatOverlay>,
    private val layoutModeProvider: () -> LayoutMode,
    private val inAppBrowserLauncher: IInAppBrowserLauncher,
    private val onExplicitLoginCallback: () -> Unit,
    private val onCancelLoginCallback: () -> Unit,
    private val childStackProvider: () -> Value<ChildStack<AppShellConfiguration, AppShellChildComponent>>
) : IHomeNavigator,
    IChatNavigator,
    IListingEditorNavigator,
    IListingDetailsNavigator,
    IProfileNavigator,
    ISettingsNavigator,
    ILoginNavigator {

    private val dashboardComponent: IDashboardComponent?
        get() = childStackProvider().value.items
            .map { it.instance }
            .filterIsInstance<AppShellChildComponent.Dashboard>()
            .firstOrNull()?.component

    @OptIn(DelicateDecomposeApi::class)
    actual override fun navigateToListingDetails(listingId: ListingId) {
        stackNavigation.pushToFront(AppShellConfiguration.ListingDetails(listingId))
    }

    actual override fun navigateToCreateListing(category: ListingCategory?) {
        stackNavigation.pushToFront(AppShellConfiguration.ListingEditor(null, category))
    }

    actual override fun navigateBack() {
        stackNavigation.pop()
    }

    actual override fun navigateToChat(jid: Jid) {
        val dashboard = dashboardComponent
        if (dashboard != null) {
            stackNavigation.popTo(0)
            dashboard.selectChat(jid)
        } else {
            stackNavigation.pushToFront(AppShellConfiguration.Chat(jid))
        }
    }

    @OptIn(DelicateDecomposeApi::class)
    actual override fun navigateToProfile(userId: UserId) {
        stackNavigation.pushToFront(AppShellConfiguration.Profile(userId))
    }

    @OptIn(DelicateDecomposeApi::class)
    actual override fun navigateToListing(listingId: ListingId) {
        stackNavigation.pushToFront(AppShellConfiguration.ListingDetails(listingId))
    }

    actual override fun navigateToChat(userId: UserId) {

    }

    @OptIn(DelicateDecomposeApi::class)
    actual override fun navigateToSettings() {
        stackNavigation.bringToFront(AppShellConfiguration.Settings)
    }

    actual override fun openUrl(url: String) {
        inAppBrowserLauncher.open(url)
    }

    actual override fun dismissLogin() {
        onCancelLoginCallback()
    }

    actual override fun navigateToEditListing(listingId: ListingId) {
        stackNavigation.pushToFront(AppShellConfiguration.ListingEditor(listingId, null))
    }

    actual override fun navigateToLogin() {
        onExplicitLoginCallback()
    }

    actual fun navigateToHome() {
        val dashboard = dashboardComponent
        if (dashboard != null) {
            stackNavigation.popTo(0)
            dashboard.onHomeTabClicked()
        } else {
            stackNavigation.popTo(0)
        }
    }

    @OptIn(DelicateDecomposeApi::class)
    actual fun onSettingsClicked() {
        stackNavigation.bringToFront(AppShellConfiguration.Settings)
    }

    actual fun onLoginClicked() {
        onExplicitLoginCallback()
    }

    actual fun dismissChatOverlay() {
        overlayNavigation.dismiss()
    }
}