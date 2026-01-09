package com.availelabs.availe.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.availelabs.availe.navigation.DeepLinkBuilder
import com.availelabs.availe.navigation.DeepLinkRoute
import com.availelabs.availe.platform.Platform
import com.availelabs.availe.platform.PlatformInfo
import com.availelabs.availe.platform.UrlHandler
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.DropdownMenuItem
import com.composables.uikit.components.Icon
import com.composables.uikit.components.Text

@Composable
fun LinkContextMenuArea(
    route: DeepLinkRoute,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val url = DeepLinkBuilder.build(route)

    ContextMenuArea(
        modifier = modifier,
        content = content,
        menuItems = { onDismiss ->
            DropdownMenuItem(
                onClick = {
                    clipboardManager.setText(AnnotatedString(url))
                    onDismiss()
                }
            ) {
                Icon(Lucide.Copy, null)
                Text("Copy link")
            }

            if (PlatformInfo.current == Platform.Web) {
                DropdownMenuItem(
                    onClick = {
                        UrlHandler.openInNewTab(url)
                        onDismiss()
                    }
                ) {
                    Icon(Lucide.ExternalLink, null)
                    Text("Open in new tab")
                }
            }
        }
    )
}