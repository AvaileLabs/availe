@file:OptIn(ExperimentalUuidApi::class)

package com.availelabs.availe.listingdetails

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.*
import coil3.compose.AsyncImage
import com.availelabs.availe.domain.extensions.canBeModifiedBy
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.extensions.toDisplayString
import com.availelabs.availe.ui.model.asString
import com.composables.icons.lucide.*
import com.composables.uikit.components.*
import com.composables.uikit.styling.*
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource
import kotlin.math.absoluteValue
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun ListingDetailsScreen(
    listingDetailsComponent: IListingDetailsComponent
) {
    val state by listingDetailsComponent.state.collectAsState()

    AppScaffold {
        Box(
            modifier = Modifier.fillMaxSize().background(Theme[colors][background])
        ) {
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
                        Text(stringResource(Res.string.generic_retry))
                    }
                }
            } else if (state.listing != null) {
                ListingDetailsContent(
                    listing = state.listing!!,
                    currentUserId = state.currentUserId,
                    onContactSeller = { sellerEmail -> listingDetailsComponent.onContactSellerClicked(sellerEmail) },
                    onProfileClicked = { userId -> listingDetailsComponent.onProfileClicked(userId) },
                    onEditClicked = { listingDetailsComponent.onEditListingClicked() }
                )
            }
        }
    }
}

@Composable
private fun ListingDetailsContent(
    listing: Listing,
    currentUserId: UserId?,
    onContactSeller: (Email) -> Unit,
    onProfileClicked: (UserId) -> Unit,
    onEditClicked: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDesktop = maxWidth > 800.dp
        if (isDesktop) {
            DesktopListingLayout(
                listing = listing,
                currentUserId = currentUserId,
                onContactSeller = onContactSeller,
                onProfileClicked = onProfileClicked,
                onEditClicked = onEditClicked
            )
        } else {
            MobileListingLayout(
                listing = listing,
                currentUserId = currentUserId,
                onContactSeller = onContactSeller,
                onProfileClicked = onProfileClicked,
                onEditClicked = onEditClicked
            )
        }
    }
}

@Composable
private fun MobileListingLayout(
    listing: Listing,
    currentUserId: UserId?,
    onContactSeller: (Email) -> Unit,
    onProfileClicked: (UserId) -> Unit,
    onEditClicked: () -> Unit
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
                            contentDescription = stringResource(Res.string.accessibility_location),
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
                    onContactClicked = onContactSeller,
                    onProfileClicked = onProfileClicked
                )
                Spacer(Modifier.height(24.dp))

                Section(stringResource(Res.string.listing_about)) {
                    Text(
                        text = listing.description?.value ?: stringResource(Res.string.listing_description_empty),
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
                        stringResource(Res.string.listing_price_total),
                        style = Theme[textStyles][body].copy(fontSize = Theme[textStyles][body].fontSize * 0.8)
                    )
                    SelectionContainer {
                        Text(
                            text = listing.price.toDisplayString(),
                            style = Theme[textStyles][title].copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                val isOwner = currentUserId != null && listing.canBeModifiedBy(currentUserId)

                if (isOwner) {
                    PrimaryButton(onClick = onEditClicked) {
                        Text("Edit Listing")
                    }
                } else {
                    PrimaryButton(onClick = { onContactSeller(Email(listing.publisher.jid.value)) }) {
                        Text(
                            if (listing.category == ListingCategory.Event) stringResource(Res.string.listing_book_ticket)
                            else stringResource(Res.string.listing_contact_seller)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopListingLayout(
    listing: Listing,
    currentUserId: UserId?,
    onContactSeller: (Email) -> Unit,
    onProfileClicked: (UserId) -> Unit,
    onEditClicked: () -> Unit
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
                            contentDescription = stringResource(Res.string.accessibility_location),
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

                Section(stringResource(Res.string.listing_about)) {
                    Text(
                        text = listing.description?.value ?: stringResource(Res.string.listing_description_empty),
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
                            onContactClicked = onContactSeller,
                            onProfileClicked = onProfileClicked
                        )

                        Spacer(Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Badge(
                                icon = Lucide.Star,
                                text = stringResource(Res.string.listing_rating_label, listing.mockRating),
                                tint = Color(0xFFEAB308)
                            )
                            Badge(
                                icon = Lucide.MessageCircle,
                                text = stringResource(Res.string.listing_reviews_label, listing.mockReviewCount),
                                tint = Theme[colors][primary]
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row {
                            Badge(
                                icon = Lucide.BadgeCheck,
                                text = stringResource(Res.string.listing_identity_verified),
                                tint = Color(0xFF10B981)
                            )
                        }

                        Spacer(Modifier.height(32.dp))

                        val isOwner = currentUserId != null && listing.canBeModifiedBy(currentUserId)

                        if (isOwner) {
                            PrimaryButton(
                                onClick = onEditClicked,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Edit Listing")
                            }
                        } else {
                            PrimaryButton(
                                onClick = { onContactSeller(Email(listing.publisher.jid.value)) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (listing.category == ListingCategory.Event) stringResource(Res.string.listing_book_now)
                                    else stringResource(Res.string.listing_contact_seller)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        SelectionContainer {
                            Text(
                                text = stringResource(Res.string.listing_secure_guarantee),
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
                        start = Offset.Zero,
                        end = Offset.Infinite
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
    onContactClicked: (Email) -> Unit,
    onProfileClicked: (UserId) -> Unit
) {
    Row(
        modifier = modifier
            .border(1.dp, Theme[colors][outline].copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { onProfileClicked(listing.publisher.id) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarUrl = listing.publisher.avatarUrl
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl.value,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            val initials = listing.publisher.displayName.value
                .split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
                .ifBlank { "?" }

            Avatar(
                painter = ColorPainter(Theme[colors][primary]),
                initials = initials,
                modifier = Modifier.size(48.dp),
                shape = CircleShape
            )
        }

        Spacer(Modifier.width(16.dp))
        SelectionContainer {
            Column {
                Text(listing.publisher.displayName.value, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Lucide.Star,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFFEAB308)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(
                            Res.string.listing_rating_count_format,
                            listing.mockRating,
                            listing.mockReviewCount
                        ),
                        style = Theme[textStyles][body].copy(fontSize = Theme[textStyles][body].fontSize * 0.85)
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            onClick = { onProfileClicked(listing.publisher.id) },
            backgroundColor = Theme[colors][card],
            contentColor = Theme[colors][onBackground],
            borderWidth = 1.dp,
            borderColor = Theme[colors][outline]
        ) {
            Text(stringResource(Res.string.listing_profile))
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