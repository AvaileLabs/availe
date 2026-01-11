package com.availelabs.availe.appshell.component

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.availelabs.availe.chat.root.IChatRootComponent
import com.availelabs.availe.dashboard.IDashboardComponent
import com.availelabs.availe.home.IHomeComponent
import com.availelabs.availe.listingdetails.IListingDetailsComponent
import com.availelabs.availe.listingeditor.IListingEditorComponent
import com.availelabs.availe.profile.IProfileComponent
import com.availelabs.availe.settings.ISettingsComponent
import com.availelabs.availe.ui.model.AppBarAction

sealed interface AppShellChildComponent {
    val actions: Value<List<AppBarAction>>

    data class Dashboard(val component: IDashboardComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = component.actions
    }

    data class Home(val component: IHomeComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = component.actions
    }

    data class Chat(val component: IChatRootComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = MutableValue(emptyList())
    }

    data class ListingEditor(val component: IListingEditorComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = component.actions
    }

    data class ListingDetails(val component: IListingDetailsComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = component.actions
    }

    data class Profile(val component: IProfileComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = component.actions
    }

    data class Settings(val component: ISettingsComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = component.actions
    }

    data class ChatOverlay(val component: IChatRootComponent) : AppShellChildComponent {
        override val actions: Value<List<AppBarAction>> = MutableValue(emptyList())
    }
}