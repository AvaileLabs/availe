package com.availelabs.availe.ui.components.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import availe.client.composeapp.generated.resources.Res
import availe.client.composeapp.generated.resources.generic_expand
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.availelabs.availe.features.chat.root.IChatRootComponent
import com.composables.icons.lucide.*
import com.composables.uikit.components.Avatar
import com.composables.uikit.components.Icon
import com.composables.uikit.components.IconButton
import com.composables.uikit.components.Text
import com.composables.uikit.styling.card
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.outline
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChatOverlayWindow(
    component: IChatRootComponent,
    title: String,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    // Local UI state for minimization
    var isCollapsed by remember { mutableStateOf(true) }

    // Auto-expand if a conversation is selected in the root component
    val panels by component.panels.subscribeAsState()
    val hasDetails = panels.details != null

    // Side-effect: If details open, ensure window is expanded
    if (hasDetails && isCollapsed) {
        isCollapsed = false
    }

    Column(
        modifier = Modifier
            .width(320.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .background(Theme[colors][card])
            .border(
                width = 1.dp,
                color = Theme[colors][outline],
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Theme[colors][card])
                .clickable { isCollapsed = !isCollapsed }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                painter = rememberVectorPainter(Lucide.MessageCircle),
                initials = "",
                modifier = Modifier.size(24.dp),
                shape = CircleShape
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { isCollapsed = !isCollapsed },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isCollapsed) Lucide.ChevronUp else Lucide.ChevronDown,
                    contentDescription = if (isCollapsed) "Expand" else "Collapse",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onExpand, // Maximize to full screen
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Lucide.Maximize2,
                    contentDescription = stringResource(Res.string.generic_expand),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Content Area (Collapsible)
        AnimatedVisibility(
            visible = !isCollapsed,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.height(500.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Theme[colors][outline])
                )
                content()
            }
        }
    }
}