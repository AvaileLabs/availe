package com.availelabs.availe.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import availe.client.designsystem.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import com.composables.uikit.components.Avatar
import com.composables.uikit.components.HorizontalDivider
import com.composables.uikit.components.Text
import com.composables.uikit.styling.card
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.outline
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChatHeader(
    isOnline: Boolean,
    recipientName: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme[colors][card])
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                painter = rememberVectorPainter(Lucide.User),
                initials = recipientName.take(1).uppercase(),
                modifier = Modifier.size(40.dp),
                shape = CircleShape
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = recipientName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (isOnline) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        SelectionContainer {
                            Text(
                                text = stringResource(Res.string.chat_status_online),
                                fontSize = 12.sp,
                                color = Theme[colors][outline].copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider()
    }
}