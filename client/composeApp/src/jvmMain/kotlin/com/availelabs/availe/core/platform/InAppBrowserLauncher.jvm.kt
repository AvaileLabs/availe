package com.availelabs.availe.core.platform

import com.availelabs.availe.platform.PlatformActivity
import java.awt.Desktop
import java.net.URI

class JvmInAppBrowserLauncher : IInAppBrowserLauncher {
    override fun open(url: String) {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(URI(url))
                } catch (_: Exception) {
                }
            }
        }
    }
}

actual fun createInAppBrowserLauncher(activity: PlatformActivity): IInAppBrowserLauncher {
    return JvmInAppBrowserLauncher()
}