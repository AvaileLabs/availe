package com.availelabs.availe.appshell.router

import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.pushToFront
import com.availelabs.availe.appshell.model.AppShellConfiguration
import com.availelabs.availe.chat.root.IChatNavigator
import com.availelabs.availe.core.platform.IInAppBrowserLauncher
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.home.IHomeNavigator
import com.availelabs.availe.listingdetails.IListingDetailsNavigator
import com.availelabs.availe.listingeditor.IListingEditorNavigator
import com.availelabs.availe.login.ILoginNavigator
import com.availelabs.availe.profile.IProfileNavigator
import com.availelabs.availe.settings.ISettingsNavigator
import com.availelabs.availe.ui.model.LayoutMode

class AppShellRouter(
    private val stackNavigation: StackNavigation<AppShellConfiguration>,
    private val overlayNavigation: SlotNavigation<AppShellConfiguration.ChatOverlay>,
    private val layoutModeProvider: () -> LayoutMode,
    private val inAppBrowserLauncher: IInAppBrowserLauncher,
    private val onExplicitLoginCallback: () -> Unit,
    private val onCancelLoginCallback: () -> Unit
) : IHomeNavigator,
    IChatNavigator,
    IListingEditorNavigator,
    IListingDetailsNavigator,
    IProfileNavigator,
    ISettingsNavigator,
    ILoginNavigator {

    // --- IHomeNavigator ---
    @OptIn(DelicateDecomposeApi::class)
    override fun navigateToListingDetails(listingId: ListingId) {
        stackNavigation.push(AppShellConfiguration.ListingDetails(listingId))
    }

    override fun navigateToCreateListing() {
        stackNavigation.pushToFront(AppShellConfiguration.ListingEditor(null))
    }

    // --- IChatNavigator & IListingEditorNavigator ---
    override fun navigateBack() {
        stackNavigation.pop()
    }

    // --- IListingDetailsNavigator ---
    override fun navigateToChat(jid: Jid) {
        if (layoutModeProvider() == LayoutMode.MultiPane) {
            overlayNavigation.activate(AppShellConfiguration.ChatOverlay(jid))
            // Note: The actual overlay component selection logic is handled reactively
            // in AppShellComponent's creation or by observing configuration changes
            // if we needed to pass data to an existing instance.
            // Since `activate` recreates/updates the slot config, the component factory
            // in AppShellComponent will receive the new JID.
        } else {
            stackNavigation.pushToFront(AppShellConfiguration.Chat(jid))
        }
    }

    @OptIn(DelicateDecomposeApi::class)
    override fun navigateToProfile(userId: UserId) {
        stackNavigation.push(AppShellConfiguration.Profile(userId))
    }

    // --- IProfileNavigator ---
    @OptIn(DelicateDecomposeApi::class)
    override fun navigateToListing(listingId: ListingId) {
        stackNavigation.push(AppShellConfiguration.ListingDetails(listingId))
    }

    override fun navigateToChat(userId: UserId) {
        // TODO: Resolve JID from user ID via repository lookup if necessary.
        // For now this path assumes the feature handles resolution or we add JID to signature later.
    }

    @OptIn(DelicateDecomposeApi::class)
    override fun navigateToSettings() {
        stackNavigation.push(AppShellConfiguration.Settings)
    }

    // --- ISettingsNavigator ---
    override fun openUrl(url: String) {
        inAppBrowserLauncher.open(url)
    }

    // --- ILoginNavigator ---
    override fun dismissLogin() {
        onCancelLoginCallback()
    }

    // --- App Shell Internal Actions ---
    @OptIn(DelicateDecomposeApi::class)
    fun onSettingsClicked() {
        stackNavigation.push(AppShellConfiguration.Settings)
    }

    fun onLoginClicked() {
        onExplicitLoginCallback()
    }

    fun dismissChatOverlay() {
        overlayNavigation.dismiss()
    }
}