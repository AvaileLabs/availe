@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.features.listingdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.availelabs.availe.domain.extensions.toDisplayString
import com.availelabs.availe.domain.model.Email
import com.availelabs.availe.domain.model.ImageUrl
import com.availelabs.availe.domain.model.Listing
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.ui.model.asString
import com.composables.icons.lucide.*
import com.composables.uikit.components.*
import com.composables.uikit.styling.*
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import kotlin.math.absoluteValue
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun ListingDetailsScreen(
    listingDetailsComponent: IListingDetailsComponent
) {
    val state by listingDetailsComponent.state.collectAsState()

    AppScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    IntermediateProgressIndicator()
                }
            } else if (state.errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    SelectionContainer {
                        Text(state.errorMessage!!.asString())
                    }
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        onClick = {
                            listingDetailsComponent.onIntent(ListingDetailsStore.Intent.RetryRequested)
                        }
                    ) {
                        Text("Retry")
                    }
                }
            } else if (state.listing != null) {
                ListingDetailsContent(
                    listing = state.listing!!,
                    onContactSeller = { sellerEmail -> listingDetailsComponent.onContactSellerClicked(sellerEmail) }
                )
            }
        }
    }
}

@Composable
private fun ListingDetailsContent(
    listing: Listing,
    onContactSeller: (Email) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDesktop = maxWidth > 800.dp
        if (isDesktop) {
            DesktopListingLayout(listing, onContactSeller)
        } else {
            MobileListingLayout(listing, onContactSeller)
        }
    }
}

@Composable
private fun MobileListingLayout(
    listing: Listing,
    onContactSeller: (Email) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                ListingImageGallery(
                    images = listing.imageLinks,
                    category = listing.category,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryChip(listing.category)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.MapPin,
                            contentDescription = "Location",
                            modifier = Modifier.size(16.dp),
                            tint = Theme[colors][onBackground].copy(alpha = 0.6f)
                        )
                        SelectionContainer {
                            Text(
                                text = listing.mockLocation,
                                style = Theme[textStyles][body].copy(
                                    fontSize = Theme[textStyles][body].fontSize * 0.9,
                                    color = Theme[colors][onBackground].copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                SelectionContainer {
                    Text(
                        text = listing.title.value,
                        style = Theme[textStyles][title].copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(Modifier.height(8.dp))

                SelectionContainer {
                    Text(
                        text = listing.price.toDisplayString(),
                        style = Theme[textStyles][title].copy(fontSize = Theme[textStyles][title].fontSize * 1.2)
                    )
                }

                Spacer(Modifier.height(24.dp))
                SellerInfoCard(
                    listing = listing,
                    modifier = Modifier.fillMaxWidth(),
                    onContactClicked = onContactSeller
                )
                Spacer(Modifier.height(24.dp))

                Section("Description") {
                    Text(
                        text = listing.description?.value ?: "No description provided.",
                        style = Theme[textStyles][body].copy(color = Theme[colors][onBackground].copy(alpha = 0.8f))
                    )
                }
            }
        }

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
                    SelectionContainer {
                        Text(
                            text = listing.price.toDisplayString(),
                            style = Theme[textStyles][title].copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                PrimaryButton(onClick = { onContactSeller(listing.commonData.publisherEmail) }) {
                    Text(if (listing.category == ListingCategory.Event) "Book Ticket" else "Contact Seller")
                }
            }
        }
    }
}

@Composable
private fun DesktopListingLayout(
    listing: Listing,
    onContactSeller: (Email) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 1200.dp)
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryChip(listing.category)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.MapPin,
                            contentDescription = "Location",
                            modifier = Modifier.size(18.dp),
                            tint = Theme[colors][onBackground].copy(alpha = 0.6f)
                        )
                        SelectionContainer {
                            Text(
                                text = listing.mockLocation,
                                style = Theme[textStyles][body].copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Theme[colors][onBackground].copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                SelectionContainer {
                    Text(
                        text = listing.title.value,
                        style = Theme[textStyles][title].copy(
                            fontSize = Theme[textStyles][title].fontSize * 1.5,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .clip(Theme[shapes][medium])
                ) {
                    ListingImageGallery(
                        images = listing.imageLinks,
                        category = listing.category,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(32.dp))

                Section("About this item") {
                    Text(
                        text = listing.description?.value ?: "No description provided.",
                        style = Theme[textStyles][body].copy(
                            lineHeight = Theme[textStyles][body].fontSize * 1.6,
                            color = Theme[colors][onBackground].copy(alpha = 0.9f)
                        )
                    )
                }
            }

            Column(modifier = Modifier.weight(0.35f)) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        SelectionContainer {
                            Text(
                                text = listing.price.toDisplayString(),
                                style = Theme[textStyles][title].copy(
                                    fontSize = Theme[textStyles][title].fontSize * 1.4,
                                    fontWeight = FontWeight.Bold,
                                    color = Theme[colors][primary]
                                )
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Theme[colors][outline].copy(alpha = 0.2f))
                        )

                        Spacer(Modifier.height(24.dp))

                        SellerInfoCard(
                            listing = listing,
                            modifier = Modifier.fillMaxWidth(),
                            onContactClicked = onContactSeller
                        )

                        Spacer(Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Badge(
                                icon = Lucide.Star,
                                text = "${listing.mockRating} Rating",
                                tint = Color(0xFFEAB308)
                            )
                            Badge(
                                icon = Lucide.MessageCircle,
                                text = "${listing.mockReviewCount} Reviews",
                                tint = Theme[colors][primary]
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row {
                            Badge(
                                icon = Lucide.BadgeCheck,
                                text = "Identity Verified",
                                tint = Color(0xFF10B981)
                            )
                        }

                        Spacer(Modifier.height(32.dp))

                        PrimaryButton(
                            onClick = { onContactSeller(listing.commonData.publisherEmail) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (listing.category == ListingCategory.Event) "Book Now" else "Contact Seller")
                        }

                        Spacer(Modifier.height(16.dp))

                        SelectionContainer {
                            Text(
                                text = "Secure transaction protected by Availe Guarantee.",
                                style = Theme[textStyles][body].copy(
                                    fontSize = Theme[textStyles][body].fontSize * 0.75,
                                    color = Theme[colors][onBackground].copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(
    icon: ImageVector,
    text: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint
        )
        Spacer(Modifier.width(6.dp))
        SelectionContainer {
            Text(
                text = text,
                style = Theme[textStyles][body].copy(
                    fontSize = Theme[textStyles][body].fontSize * 0.85,
                    fontWeight = FontWeight.Medium
                )
            )
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
private fun SellerInfoCard(
    listing: Listing,
    modifier: Modifier = Modifier,
    onContactClicked: (Email) -> Unit
) {
    Row(
        modifier = modifier
            .border(1.dp, Theme[colors][outline].copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            painter = ColorPainter(Theme[colors][primary]),
            initials = "AS",
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.width(16.dp))
        SelectionContainer {
            Column {
                Text("Alice Seller", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Lucide.Star,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFFEAB308)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${listing.mockRating} (${listing.mockReviewCount})",
                        style = Theme[textStyles][body].copy(fontSize = Theme[textStyles][body].fontSize * 0.85)
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            onClick = { onContactClicked(listing.commonData.publisherEmail) },
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
        SelectionContainer {
            Text(
                text = sectionTitle,
                style = Theme[textStyles][title].copy(fontSize = Theme[textStyles][title].fontSize * 0.9)
            )
        }
        Spacer(Modifier.height(12.dp))
        SelectionContainer {
            content()
        }
    }
}

private val Listing.category: ListingCategory
    get() = when (this) {
        is Listing.Item -> ListingCategory.Item
        is Listing.Event -> ListingCategory.Event
        is Listing.Service -> ListingCategory.Service
    }

private val Listing.mockLocation: String
    get() {
        val locations = listOf(
            "New York, NY",
            "San Francisco, CA",
            "Austin, TX",
            "London, UK",
            "Remote",
            "Chicago, IL",
            "Seattle, WA",
            "Toronto, Canada"
        )
        return locations[this.id.value.hashCode().absoluteValue % locations.size]
    }

private val Listing.mockRating: String
    get() {
        val baseRating = 4.0
        val variance = (this.id.value.hashCode().absoluteValue % 10) / 10.0
        return (baseRating + variance).toString().take(3)
    }

private val Listing.mockReviewCount: Int
    get() = (this.id.value.hashCode().absoluteValue % 200) + 12