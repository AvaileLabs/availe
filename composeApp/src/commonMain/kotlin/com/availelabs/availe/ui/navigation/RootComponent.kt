package com.availelabs.availe.ui.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.availelabs.availe.di.ApplicationContainer
import com.availelabs.availe.ui.screens.home.HomeScreenComponent
import com.availelabs.availe.ui.screens.home.IHomeScreenComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface IRootComponent {
    val childStack: Value<ChildStack<*, Child>>
}

class RootComponent(
    componentContext: ComponentContext,
    private val appContainer: ApplicationContainer
) : IRootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Configuration>()
    private val scope = componentContext.coroutineScope(Dispatchers.Main.immediate)

    override val childStack = childStack(
        source = navigation,
        serializer = Configuration.serializer(),
        initialConfiguration = Configuration.Home,
        childFactory = ::createChild,
    )

    private fun createChild(
        configuration: Configuration,
        componentContext: ComponentContext
    ): Child {
        return when (configuration) {
            is Configuration.Home -> Child.Home(createHomeScreenComponent(componentContext))
        }
    }

    private fun createHomeScreenComponent(componentContext: ComponentContext): IHomeScreenComponent {
        return HomeScreenComponent(
            componentContext = componentContext,
            onLogout = {
                scope.launch {
                    appContainer.authenticationService.logout()
                }
            }
        )
    }
}

sealed interface Child {
    data class Home(val component: IHomeScreenComponent) : Child
}

@Serializable
sealed interface Configuration {
    @Serializable
    data object Home : Configuration
}