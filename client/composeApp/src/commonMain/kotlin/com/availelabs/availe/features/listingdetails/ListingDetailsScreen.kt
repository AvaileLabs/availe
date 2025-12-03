package com.availelabs.availe.features.listingdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.availelabs.availe.domain.extensions.toDisplayString
import com.availelabs.availe.domain.model.ImageUrl
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingCategory
import com.composables.icons.lucide.*
import com.composables.uikit.components.*
import com.composables.uikit.styling.*
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme

@Composable
fun ListingDetailsScreen(
    listingDetailsComponent: IListingDetailsComponent
) {
    val state by listingDetailsComponent.state.collectAsState()

    AppScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        IntermediateProgressIndicator()
                    }
                }

                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(state.errorMessage!!)
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(
                            onClick = {
                                listingDetailsComponent.onIntent(ListingDetailsStore.Intent.RetryRequested)
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }

                state.listing != null -> {
                    ListingDetailsContent(
                        listing = state.listing!!,
                        onBack = listingDetailsComponent::onBackClicked
                    )
                }
            }
        }
    }
}

@Composable
private fun ListingDetailsContent(
    listing: Listing,
    onBack: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDesktop = maxWidth > 800.dp
        if (isDesktop) {
            DesktopListingLayout(listing, onBack)
        } else {
            MobileListingLayout(listing, onBack)
        }
    }
}

@Composable
private fun MobileListingLayout(
    listing: Listing,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp) // Space for bottom bar
        ) {
            // Hero Image
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                ListingImageGallery(
                    images = listing.imageLinks,
                    category = listing.category,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Actions Overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(
                            top = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        ), // Safe area
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        backgroundColor = Theme[colors][card].copy(alpha = 0.8f),
                        shape = CircleShape
                    ) {
                        Icon(Lucide.ArrowLeft, "Back")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {},
                            backgroundColor = Theme[colors][card].copy(alpha = 0.8f),
                            shape = CircleShape
                        ) {
                            Icon(Lucide.Share, "Share")
                        }
                        IconButton(
                            onClick = {},
                            backgroundColor = Theme[colors][card].copy(alpha = 0.8f),
                            shape = CircleShape
                        ) {
                            Icon(Lucide.Heart, "Favorite")
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Spacer(Modifier.height(8.dp))
                CategoryChip(listing.category)
                Spacer(Modifier.height(12.dp))

                Text(
                    text = listing.title.value,
                    style = Theme[textStyles][title].copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = listing.price.toDisplayString(),
                    style = Theme[textStyles][title].copy(fontSize = Theme[textStyles][title].fontSize * 1.2)
                )

                Spacer(Modifier.height(24.dp))
                SellerInfoCard(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))

                Section("Description") {
                    Text(
                        text = listing.description?.value ?: "No description provided.",
                        style = Theme[textStyles][body].copy(color = Theme[colors][onBackground].copy(alpha = 0.8f))
                    )
                }
            }
        }

        // Sticky Bottom Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Theme[colors][card])
                .border(1.dp, Theme[colors][outline].copy(alpha = 0.1f))
                .padding(16.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Total Price",
                        style = Theme[textStyles][body].copy(fontSize = Theme[textStyles][body].fontSize * 0.8)
                    )
                    Text(
                        text = listing.price.toDisplayString(),
                        style = Theme[textStyles][title].copy(fontWeight = FontWeight.Bold)
                    )
                }
                PrimaryButton(onClick = {}) {
                    Text(if (listing.category == ListingCategory.Event) "Book Ticket" else "Contact Seller")
                }
            }
        }
    }
}

@Composable
private fun DesktopListingLayout(
    listing: Listing,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Desktop Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Lucide.ArrowLeft, "Back")
            }
            Spacer(Modifier.width(16.dp))
            Text("Back to listings", style = Theme[textStyles][body].copy(fontWeight = FontWeight.Medium))
        }

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            // Left Column: Gallery & Description
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f)
                        .clip(Theme[shapes][medium])
                ) {
                    ListingImageGallery(
                        images = listing.imageLinks,
                        category = listing.category,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(32.dp))

                Section("Description") {
                    Text(
                        text = listing.description?.value ?: "No description provided.",
                        style = Theme[textStyles][body].copy(lineHeight = Theme[textStyles][body].fontSize * 1.5)
                    )
                }

                Spacer(Modifier.height(32.dp))
                Section("Seller") {
                    SellerInfoCard(modifier = Modifier.widthIn(max = 400.dp))
                }
            }

            // Right Column: Sticky Action Card
            Column(modifier = Modifier.weight(0.4f)) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        CategoryChip(listing.category)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = listing.title.value,
                            style = Theme[textStyles][title].copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = listing.price.toDisplayString(),
                            style = Theme[textStyles][title].copy(
                                fontWeight = FontWeight.Normal,
                                color = Theme[colors][primary]
                            )
                        )
                        Spacer(Modifier.height(32.dp))
                        PrimaryButton(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (listing.category == ListingCategory.Event) "Book Now" else "Contact Seller")
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Secure transaction protected by Availe Guarantee.",
                            style = Theme[textStyles][body].copy(fontSize = Theme[textStyles][body].fontSize * 0.8),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListingImageGallery(
    images: List<ImageUrl>,
    category: ListingCategory,
    modifier: Modifier
) {
    if (images.isNotEmpty()) {
        AsyncImage(
            model = images.first().value,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        CategoryPlaceholder(category, modifier)
    }
}

@Composable
private fun CategoryPlaceholder(
    category: ListingCategory,
    modifier: Modifier
) {
    val (backgroundColor, icon) = when (category) {
        ListingCategory.Item -> Color(0xFFE0E7FF) to Lucide.Box
        ListingCategory.Event -> Color(0xFFF3E8FF) to Lucide.Calendar
        ListingCategory.Service -> Color(0xFFD1FAE5) to Lucide.User
    }

    val iconColor = when (category) {
        ListingCategory.Item -> Color(0xFF4338CA)
        ListingCategory.Event -> Color(0xFF7E22CE)
        ListingCategory.Service -> Color(0xFF047857)
    }

    Box(
        modifier = modifier.background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Pattern overlay effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset.Infinite
                    )
                )
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = iconColor
        )
    }
}

@Composable
private fun CategoryChip(category: ListingCategory) {
    Chip(
        backgroundColor = Theme[colors][outline].copy(alpha = 0.1f),
        contentColor = Theme[colors][onBackground]
    ) {
        Text(category.name.uppercase(), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SellerInfoCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .border(1.dp, Theme[colors][outline].copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            painter = androidx.compose.ui.graphics.painter.ColorPainter(Theme[colors][primary]),
            initials = "AS",
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text("Alice Seller", fontWeight = FontWeight.Bold)
            Text(
                "Joined 2024",
                style = Theme[textStyles][body].copy(fontSize = Theme[textStyles][body].fontSize * 0.85)
            )
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            onClick = {},
            backgroundColor = Theme[colors][card],
            contentColor = Theme[colors][onBackground],
            borderWidth = 1.dp,
            borderColor = Theme[colors][outline]
        ) {
            Text("Profile")
        }
    }
}

@Composable
private fun Section(sectionTitle: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = sectionTitle,
            style = Theme[textStyles][title].copy(fontSize = Theme[textStyles][title].fontSize * 0.9)
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

private val Listing.category: ListingCategory
    get() = when (this) {
        is Listing.Item -> ListingCategory.Item
        is Listing.Event -> ListingCategory.Event
        is Listing.Service -> ListingCategory.Service
    }