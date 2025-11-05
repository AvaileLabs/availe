import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.availelabs.availe.ui.screens.home.ListingItem

@Composable
internal fun ListingGrid(
    listings: List<ListingItem>,
    onItemClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(listings) { item ->
            ListingCard(
                item = item,
                onItemClick = onItemClick,
                onFavoriteClick = onFavoriteClick
            )
        }
    }
}