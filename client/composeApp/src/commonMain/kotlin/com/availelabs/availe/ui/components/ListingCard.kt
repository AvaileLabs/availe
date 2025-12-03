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
import com.availelabs.availe.domain.model.*
import com.composables.icons.lucide.*
import com.composables.uikit.components.Icon
import com.composables.uikit.components.Text
import com.composables.uikit.styling.card
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.outline
import com.composeunstyled.theme.Theme

@Composable
fun ListingCard(
    title: ListingTitle,
    price: Price,
    imageUrl: ImageUrl?,
    category: ListingCategory,
    description: ListingDescription?,
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
                ListingContent(title, price, category, description, Modifier.padding(16.dp))
            }
        } else {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                ListingImage(imageUrl, category, Modifier.width(250.dp).fillMaxHeight())
                ListingContent(title, price, category, description, Modifier.padding(16.dp).weight(1f))
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
        val icon = when (category) {
            ListingCategory.Event -> Lucide.Calendar
            ListingCategory.Service -> Lucide.User
            ListingCategory.Item -> Lucide.Image
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
    title: ListingTitle,
    price: Price,
    category: ListingCategory,
    description: ListingDescription?,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon = when (category) {
                ListingCategory.Event -> Lucide.Calendar
                ListingCategory.Service -> Lucide.User
                ListingCategory.Item -> Lucide.Box
            }
            Icon(icon, null, Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(category.name.uppercase())
            Spacer(Modifier.weight(1f))
            Icon(Lucide.Heart, "Favorite", Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(title.value)
        Spacer(Modifier.height(4.dp))

        val currencySymbol = SupportedCurrency.fromCode(price.currency)?.symbol ?: price.currency.value
        Text("$currencySymbol${price.amount.value}")

        if (description != null) {
            Spacer(Modifier.height(8.dp))
            Text(description.value, maxLines = 2)
        }
    }
}