package com.availelabs.availe.appshell.strategy

import com.availelabs.availe.appshell.component.AppShellChildComponent
import com.availelabs.availe.appshell.component.AppShellChildFactoryContext
import com.availelabs.availe.appshell.model.AppShellConfiguration
import com.availelabs.availe.appshell.model.AppShellIntent

expect object AppShellNavigationStrategy {
    fun calculateInitialStack(intent: AppShellIntent): List<AppShellConfiguration>

    fun createChild(
        configuration: AppShellConfiguration,
        context: AppShellChildFactoryContext
    ): AppShellChildComponent
}