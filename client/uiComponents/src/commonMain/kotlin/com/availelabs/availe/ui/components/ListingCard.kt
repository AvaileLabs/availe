package com.availelabs.availe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.status_going
import coil3.compose.AsyncImage
import com.availelabs.availe.domain.model.*
import com.composables.icons.lucide.*
import com.composables.uikit.components.Icon
import com.composables.uikit.components.Text
import com.composables.uikit.styling.body
import com.composables.uikit.styling.card
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.outline
import com.composables.uikit.styling.textStyles
import com.composeunstyled.theme.Theme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
fun ListingCard(
    title: ListingTitle,
    price: Price,
    imageUrl: ImageUrl?,
    category: ListingCategory,
    description: ListingDescription?,
    startTime: Instant?,
    isRsvped: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Theme[colors][card])
            .border(1.dp, Theme[colors][outline], RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        val isCompact = maxWidth < 600.dp

        if (isCompact) {
            Column {
                ListingImage(imageUrl, category, Modifier.fillMaxWidth().height(200.dp))
                ListingContent(title, price, description, startTime, isRsvped, Modifier.padding(16.dp))
            }
        } else {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                ListingImage(imageUrl, category, Modifier.width(250.dp).fillMaxHeight())
                ListingContent(title, price, description, startTime, isRsvped, Modifier.padding(16.dp).weight(1f))
            }
        }
    }
}

@Composable
private fun ListingImage(url: ImageUrl?, category: ListingCategory, modifier: Modifier) {
    Box(modifier = modifier) {
        if (url != null && url.value.isNotBlank()) {
            AsyncImage(
                model = url.value,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            MockImagePlaceholder(category, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun MockImagePlaceholder(category: ListingCategory, modifier: Modifier) {
    Box(
        modifier = modifier.background(Theme[colors][outline].copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Lucide.Calendar,
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun ListingContent(
    title: ListingTitle,
    price: Price,
    description: ListingDescription?,
    startTime: Instant?,
    isRsvped: Boolean,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.Calendar, null, Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            val dateString = startTime?.let { formatEventTime(it) } ?: "TBD"
            Text(dateString, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (isRsvped) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.CircleCheck, null, Modifier.size(16.dp), tint = Color(0xFF10B981))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(Res.string.status_going),
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        style = Theme[textStyles][body].copy(fontSize = Theme[textStyles][body].fontSize * 0.85)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SelectionContainer {
            Text(title.value)
        }
        Spacer(Modifier.height(4.dp))

        SelectionContainer {
            val currencySymbol = SupportedCurrency.fromCode(price.currency)?.symbol ?: price.currency.value
            Text("$currencySymbol${price.amount.value}")
        }

        if (description != null) {
            Spacer(Modifier.height(8.dp))
            SelectionContainer {
                Text(description.value, maxLines = 2)
            }
        }
    }
}

enum class TimeFormat {
    Standard,
    Military
}

fun formatEventTime(instant: Instant, format: TimeFormat = TimeFormat.Standard): String {
    return try {
        val kotlinxInstant = kotlinx.datetime.Instant.parse(instant.toString())
        val localDateTime = kotlinxInstant.toLocalDateTime(TimeZone.currentSystemDefault())

        val month = localDateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercaseChar() }
        val day = localDateTime.dayOfMonth

        val hour = localDateTime.hour
        val minute = localDateTime.minute.toString().padStart(2, '0')

        when (format) {
            TimeFormat.Military -> {
                val hourString = hour.toString().padStart(2, '0')
                "$month $day • $hourString:$minute"
            }
            TimeFormat.Standard -> {
                val isPm = hour >= 12
                val standardHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                val amPm = if (isPm) "PM" else "AM"
                "$month $day • $standardHour:$minute $amPm"
            }
        }
    } catch (exception: Exception) {
        instant.toString()
    }
}