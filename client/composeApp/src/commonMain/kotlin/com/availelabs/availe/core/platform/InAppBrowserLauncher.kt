package com.availelabs.availe.core.platform

import com.availelabs.availe.platform.PlatformActivity

interface IInAppBrowserLauncher {
    fun open(url: String)
}

expect fun createInAppBrowserLauncher(activity: PlatformActivity): IInAppBrowserLauncher