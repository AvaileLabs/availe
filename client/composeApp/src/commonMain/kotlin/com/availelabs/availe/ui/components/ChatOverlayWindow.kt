package com.availelabs.availe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize2
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.X
import com.composables.uikit.components.Avatar
import com.composables.uikit.components.Icon
import com.composables.uikit.components.IconButton
import com.composables.uikit.components.Text
import com.composables.uikit.styling.card
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.outline
import com.composeunstyled.theme.Theme

@Composable
fun ChatOverlayWindow(
    title: String,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .width(360.dp)
            .height(500.dp)
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
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Theme[colors][card])
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
                    onClick = onExpand,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Maximize2,
                        contentDescription = "Expand",
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Theme[colors][outline])
            )

            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}