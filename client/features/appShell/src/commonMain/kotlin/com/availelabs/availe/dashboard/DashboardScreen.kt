package com.availelabs.availe.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.chat.root.ChatRootScreen
import com.availelabs.availe.home.HomeScreen
import com.availelabs.availe.homenav.HomeNavChildComponent
import com.availelabs.availe.listingdetails.ListingDetailsScreen
import com.availelabs.availe.profile.ProfileScreen
import com.availelabs.availe.profilenav.ProfileNavChildComponent
import com.availelabs.availe.settings.SettingsScreen
import com.availelabs.availe.ui.animation.appStackAnimation
import com.availelabs.availe.ui.util.rememberSyncedPagerState

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun DashboardScreen(
    component: IDashboardComponent,
    isSwipeEnabled: Boolean
) {
    val pages by component.pages.subscribeAsState()
    val childPages = pages.items

    val pagerState = rememberSyncedPagerState(
        pagesValue = component.pages,
        onPageSelected = component::onPageSelected
    )

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false // Root swipe disabled
    ) { pageIndex ->
        val child = childPages[pageIndex]

        when (val instance = child.instance) {
            is DashboardChildComponent.HomeNav -> {
                val childStack by instance.component.childStack.subscribeAsState()

                Children(
                    stack = childStack,
                    modifier = Modifier.fillMaxSize(),
                    animation = appStackAnimation(
                        backHandler = instance.component.backHandler,
                        onBack = instance.component::onBackClicked
                    )
                ) {
                    when (val childInstance = it.instance) {
                        is HomeNavChildComponent.HomeFeed -> {
                            HomeScreen(
                                homeComponent = childInstance.component,
                                onSearchClick = {},
                                onNotificationClick = {}
                            )
                        }

                        is HomeNavChildComponent.ListingDetails -> {
                            ListingDetailsScreen(
                                listingDetailsComponent = childInstance.component
                            )
                        }

                        is HomeNavChildComponent.Profile -> {
                            ProfileScreen(component = childInstance.component)
                        }
                    }
                }
            }

            is DashboardChildComponent.ChatRoot -> {
                ChatRootScreen(
                    component = instance.component,
                    modifier = Modifier.fillMaxSize()
                )
            }

            is DashboardChildComponent.ProfileNav -> {
                val childStack by instance.component.childStack.subscribeAsState()

                Children(
                    stack = childStack,
                    modifier = Modifier.fillMaxSize(),
                    animation = appStackAnimation(
                        backHandler = instance.component.backHandler,
                        onBack = instance.component::onBackClicked
                    )
                ) {
                    when (val childInstance = it.instance) {
                        is ProfileNavChildComponent.Profile -> {
                            ProfileScreen(component = childInstance.component)
                        }

                        is ProfileNavChildComponent.ListingDetails -> {
                            ListingDetailsScreen(listingDetailsComponent = childInstance.component)
                        }

                        is ProfileNavChildComponent.Settings -> {
                            SettingsScreen(component = childInstance.component)
                        }
                    }
                }
            }

            null -> {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}