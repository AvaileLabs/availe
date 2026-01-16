package com.availelabs.availe.ui.components.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.generic_collapse
import availe.client.designsystem.generated.resources.generic_expand
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
    title: String,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    var isCollapsed by remember { mutableStateOf(true) }

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

            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { isCollapsed = !isCollapsed },
                modifier = Modifier.size(32.dp)
            ) {
                val icon = if (isCollapsed) Lucide.ChevronUp else Lucide.ChevronDown
                val description = if (isCollapsed) {
                    stringResource(Res.string.generic_expand)
                } else {
                    stringResource(Res.string.generic_collapse)
                }

                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onExpand,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Lucide.Maximize2,
                    contentDescription = stringResource(Res.string.generic_expand),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

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