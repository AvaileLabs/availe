package com.availelabs.availe.appshell.router

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.value.Value
import com.availelabs.availe.appshell.component.AppShellChildComponent
import com.availelabs.availe.appshell.model.AppShellConfiguration
import com.availelabs.availe.chat.root.IChatNavigator
import com.availelabs.availe.core.platform.IInAppBrowserLauncher
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

expect class AppShellRouter(
    stackNavigation: StackNavigation<AppShellConfiguration>,
    overlayNavigation: SlotNavigation<AppShellConfiguration.ChatOverlay>,
    layoutModeProvider: () -> LayoutMode,
    inAppBrowserLauncher: IInAppBrowserLauncher,
    onExplicitLoginCallback: () -> Unit,
    onCancelLoginCallback: () -> Unit,
    childStackProvider: () -> Value<ChildStack<AppShellConfiguration, AppShellChildComponent>>
) : IHomeNavigator,
    IChatNavigator,
    IListingEditorNavigator,
    IListingDetailsNavigator,
    IProfileNavigator,
    ISettingsNavigator,
    ILoginNavigator {

    override fun navigateToListingDetails(listingId: ListingId)
    override fun navigateToCreateListing(category: ListingCategory?)
    override fun navigateBack()
    override fun navigateToChat(jid: Jid)
    override fun navigateToProfile(userId: UserId)
    override fun navigateToListing(listingId: ListingId)
    override fun navigateToSettings()
    override fun openUrl(url: String)
    override fun dismissLogin()
    override fun navigateToEditListing(listingId: ListingId)
    override fun navigateToLogin()

    fun navigateToHome()
    fun onSettingsClicked()
    fun onLoginClicked()
    fun dismissChatOverlay()
}