package com.availelabs.availe.features.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.availelabs.availe.features.home.ListingItem
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.Card
import com.composables.uikit.components.Icon
import com.composables.uikit.components.Text
import com.composables.uikit.styling.accent
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.outline
import com.composeunstyled.theme.Theme

@Composable
internal fun ListingCard(
    item: ListingItem,
    onItemClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit
) {
    Card(
        onClick = { onItemClick(item.id) }
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.Image,
                contentDescription = "Image placeholder",
                modifier = Modifier.size(64.dp),
                tint = Theme[colors][outline].copy(alpha = 0.5f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.title)
                Text(item.price, color = Theme[colors][accent])
            }
            Icon(
                imageVector = Lucide.Heart,
                contentDescription = "Favorite",
                modifier = Modifier.clickable { onFavoriteClick(item.id) },
                tint = if (item.isFavorited) Color.Red else Color.Gray
            )
        }
    }
}