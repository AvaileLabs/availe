package com.availelabs.availe.ui.components.listings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.availelabs.availe.platform.Platform
import com.availelabs.availe.platform.PlatformInfo
import com.availelabs.availe.platform.UrlHandler
import com.availelabs.availe.ui.components.ContextMenuArea
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.DropdownMenuItem
import com.composables.uikit.components.Icon
import com.composables.uikit.components.Text

@Composable
fun ListingContextMenu(
    url: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

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