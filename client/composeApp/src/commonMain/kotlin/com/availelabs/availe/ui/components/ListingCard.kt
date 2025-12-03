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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.*
import com.composables.uikit.components.Icon
import com.composables.uikit.components.Text
import com.composables.uikit.styling.card
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.outline
import com.composeunstyled.theme.Theme

@Composable
fun ListingCard(
    title: String,
    price: String,
    imageUrl: String?,
    type: String,
    description: String?,
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
                ListingImage(imageUrl, type, Modifier.fillMaxWidth().height(200.dp))
                ListingContent(title, price, type, description, Modifier.padding(16.dp))
            }
        } else {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                ListingImage(imageUrl, type, Modifier.width(250.dp).fillMaxHeight())
                ListingContent(title, price, type, description, Modifier.padding(16.dp).weight(1f))
            }
        }
    }
}

@Composable
private fun ListingImage(url: String?, type: String, modifier: Modifier) {
    Box(modifier = modifier) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            MockImagePlaceholder(type, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun MockImagePlaceholder(type: String, modifier: Modifier) {
    Box(
        modifier = modifier.background(Theme[colors][outline].copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        val icon = when (type) {
            "Event" -> Lucide.Calendar
            "Service" -> Lucide.User
            else -> Lucide.Image
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun ListingContent(
    title: String,
    price: String,
    type: String,
    description: String?,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon = when (type) {
                "Event" -> Lucide.Calendar
                "Service" -> Lucide.User
                else -> Lucide.Box
            }
            Icon(icon, null, Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(type.uppercase())
            Spacer(Modifier.weight(1f))
            Icon(Lucide.Heart, "Favorite", Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(title)
        Spacer(Modifier.height(4.dp))
        Text(price)
        if (description != null) {
            Spacer(Modifier.height(8.dp))
            Text(description, maxLines = 2)
        }
    }
}