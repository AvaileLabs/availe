package com.availelabs.availe.core.platform

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

class AndroidInAppBrowserLauncher(
    private val activity: PlatformActivity
) : IInAppBrowserLauncher {
    override fun open(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(activity, Uri.parse(url))
    }
}

actual fun createInAppBrowserLauncher(activity: PlatformActivity): IInAppBrowserLauncher {
    return AndroidInAppBrowserLauncher(activity)
}