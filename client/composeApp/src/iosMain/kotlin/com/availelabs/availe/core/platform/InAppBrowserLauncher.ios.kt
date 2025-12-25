package com.availelabs.availe.core.platform

import com.availelabs.availe.configuration.SharedAuthenticationConstants
import com.availelabs.availe.platform.PlatformActivity
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject

class IosInAppBrowserLauncher(
    private val callbackUrlScheme: String
) : IInAppBrowserLauncher {
    private var webAuthenticationSession: ASWebAuthenticationSession? = null

    override fun open(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return

        val keyWindow = UIApplication.sharedApplication.windows.firstOrNull {
            (it as? UIWindow)?.isKeyWindow() == true
        } as? UIWindow ?: return

        val session = ASWebAuthenticationSession(
            uRL = nsUrl,
            callbackURLScheme = callbackUrlScheme,
            completionHandler = { _: NSURL?, _: NSError? ->
                webAuthenticationSession = null
            }
        )

        session.prefersEphemeralWebBrowserSession = false
        session.presentationContextProvider = object : NSObject(),
            ASWebAuthenticationPresentationContextProvidingProtocol {
            override fun presentationAnchorForWebAuthenticationSession(
                session: ASWebAuthenticationSession
            ): ASPresentationAnchor {
                return keyWindow
            }
        }

        webAuthenticationSession = session
        session.start()
    }
}

actual fun createInAppBrowserLauncher(activity: PlatformActivity): IInAppBrowserLauncher {
    return IosInAppBrowserLauncher(
        callbackUrlScheme = SharedAuthenticationConstants.REDIRECT_SCHEME
    )
}