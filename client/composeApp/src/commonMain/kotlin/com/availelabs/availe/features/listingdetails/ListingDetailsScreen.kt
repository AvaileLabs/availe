package com.availelabs.availe.features.listingdetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.availelabs.availe.domain.extensions.toDisplayString
import com.composables.uikit.components.AppScaffold
import com.composables.uikit.components.IntermediateProgressIndicator
import com.composables.uikit.components.PrimaryButton
import com.composables.uikit.components.Text
import com.composables.uikit.styling.body
import com.composables.uikit.styling.textStyles
import com.composables.uikit.styling.title
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
                    val listing = state.listing!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (listing.imageLinks.isNotEmpty()) {
                            AsyncImage(
                                model = listing.imageLinks.first().value,
                                contentDescription = listing.title.value,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = listing.title.value,
                                style = Theme[textStyles][title]
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = listing.price.toDisplayString(),
                                style = Theme[textStyles][title].copy(fontWeight = FontWeight.Normal)
                            )
                            Spacer(Modifier.height(16.dp))
                            if (listing.description != null) {
                                Text(
                                    text = listing.description!!.value,
                                    style = Theme[textStyles][body]
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}