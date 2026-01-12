package com.availelabs.availe.appshell.component

import com.arkivanov.decompose.ComponentContext
import com.availelabs.availe.appshell.model.AppShellIntent
import com.availelabs.availe.appshell.router.AppShellRouter
import com.availelabs.availe.chat.details.IChatComponent
import com.availelabs.availe.chat.root.IChatRootComponent
import com.availelabs.availe.di.IApplicationContainer
import com.availelabs.availe.domain.model.Jid
import com.availelabs.availe.domain.model.ListingId
import com.availelabs.availe.domain.model.UserId
import com.availelabs.availe.home.IHomeComponent
import com.availelabs.availe.homenav.HomeNavConfiguration
import com.availelabs.availe.homenav.IHomeNavComponent
import com.availelabs.availe.listingdetails.IListingDetailsComponent
import com.availelabs.availe.model.ListingFeedType
import com.availelabs.availe.profile.IProfileComponent
import com.availelabs.availe.profilenav.IProfileNavComponent
import com.availelabs.availe.profilenav.ProfileNavConfiguration
import com.availelabs.availe.settings.ISettingsComponent

data class AppShellChildFactoryContext(
    val componentContext: ComponentContext,
    val appContainer: IApplicationContainer,
    val navigator: AppShellRouter,
    val initialIntent: AppShellIntent,
    val onShowLoginGuard: () -> Unit,
    val homeNavFactory: (ComponentContext, List<HomeNavConfiguration>) -> IHomeNavComponent,
    val chatRootFactory: (ComponentContext, Jid?) -> IChatRootComponent,
    val profileNavFactory: (ComponentContext, UserId, List<ProfileNavConfiguration>) -> IProfileNavComponent,
    val homeFactory: (ComponentContext, ListingFeedType) -> IHomeComponent,
    val listingDetailsFactory: (ComponentContext, ListingId) -> IListingDetailsComponent,
    val profileFactory: (ComponentContext, UserId) -> IProfileComponent,
    val settingsFactory: (ComponentContext) -> ISettingsComponent,
    val chatFactory: (ComponentContext, Jid) -> IChatComponent
)