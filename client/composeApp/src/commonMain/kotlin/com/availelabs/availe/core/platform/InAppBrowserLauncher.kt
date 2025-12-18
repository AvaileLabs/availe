package com.availelabs.availe.core.platform

interface IInAppBrowserLauncher {
    fun open(url: String)
}

expect fun createInAppBrowserLauncher(activity: PlatformActivity): IInAppBrowserLauncher