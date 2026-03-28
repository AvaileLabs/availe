package com.availelabs.availe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.availelabs.availe.ui.mapper.EventProximityUiMapper
import com.availelabs.availe.ui.model.AgendaUiModel
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.Icon
import com.composables.uikit.components.Text
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme

@Composable
fun AgendaCard(
    agendaUiModel: AgendaUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val proximityUiConfiguration = EventProximityUiMapper.map(agendaUiModel.proximity)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Theme[colors][card])
            .border(1.dp, Theme[colors][outline], RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Theme[colors][outline].copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (agendaUiModel.imageUrl != null && agendaUiModel.imageUrl.value.isNotBlank()) {
                AsyncImage(
                    model = agendaUiModel.imageUrl.value,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Lucide.Calendar,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            StatusBadge(
                text = proximityUiConfiguration.text,
                icon = proximityUiConfiguration.icon,
                backgroundColor = proximityUiConfiguration.backgroundColor,
                contentColor = proximityUiConfiguration.contentColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = agendaUiModel.title.value,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formatEventTime(agendaUiModel.startTime),
                style = Theme[textStyles][body].copy(
                    fontSize = Theme[textStyles][body].fontSize * 0.85f
                )
            )
        }
    }
}