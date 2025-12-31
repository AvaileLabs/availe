package com.availelabs.availe.features.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.features.home.HomeScreen
import com.availelabs.availe.features.listingdetails.ListingDetailsScreen
import com.availelabs.availe.features.profile.ProfileScreen
import com.availelabs.availe.features.profilenav.ProfileNavChildComponent
import com.availelabs.availe.features.settings.SettingsScreen
import com.availelabs.availe.ui.animation.appStackAnimation
import com.availelabs.availe.ui.components.PredictiveBackGestureOverlay
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

                // Note: We use Children here to render the stack inside the tab
                Children(
                    stack = childStack,
                    modifier = Modifier.fillMaxSize(),
                    animation = appStackAnimation(
                        backHandler = instance.component.backHandler,
                        onBack = instance.component::onBackClicked
                    )
                ) {
                    when (val childInstance = it.instance) {
                        is com.availelabs.availe.features.homenav.HomeNavChildComponent.HomeFeed -> {
                            HomeScreen(
                                homeComponent = childInstance.component,
                                onSearchClick = {},
                                onNotificationClick = {}
                            )
                        }

                        is com.availelabs.availe.features.homenav.HomeNavChildComponent.ListingDetails -> {
                            // Back gesture enabled only for detail screens
                            PredictiveBackGestureOverlay(
                                backDispatcher = null, // Handled by stack
                                backIcon = null,
                                modifier = Modifier.fillMaxSize(),
                                content = {
                                    ListingDetailsScreen(
                                        listingDetailsComponent = childInstance.component
                                    )
                                }
                            )
                        }

                        is com.availelabs.availe.features.homenav.HomeNavChildComponent.Profile -> {
                            ProfileScreen(component = childInstance.component)
                        }
                    }
                }
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