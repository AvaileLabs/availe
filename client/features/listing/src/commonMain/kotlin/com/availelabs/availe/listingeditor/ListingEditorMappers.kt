package com.availelabs.availe.listingeditor

import availe.client.designsystem.generated.resources.*
import com.availelabs.availe.DraftEventBounds
import com.availelabs.availe.ListingEditorError
import com.availelabs.availe.ListingEditorState
import com.availelabs.availe.domain.model.*
import com.availelabs.availe.dto.kreplica.*
import com.availelabs.availe.model.ListingDraft
import com.availelabs.availe.ui.model.UiText
import io.availe.models.Patchable
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.StringResource

val ListingEditorPageConfig.titleRes: StringResource
    get() = when (this) {
        ListingEditorPageConfig.Type -> Res.string.listing_editor_step_type
        ListingEditorPageConfig.Basics -> Res.string.listing_editor_step_basics
        ListingEditorPageConfig.Schedule -> Res.string.listing_editor_step_schedule
        ListingEditorPageConfig.Pricing -> Res.string.listing_editor_step_pricing
        ListingEditorPageConfig.Media -> Res.string.listing_editor_step_media
        ListingEditorPageConfig.Review -> Res.string.listing_editor_step_review
    }

val ListingCategory.titleRes: StringResource
    get() = when (this) {
        ListingCategory.Item -> Res.string.category_item
        ListingCategory.Event -> Res.string.category_event
        ListingCategory.Service -> Res.string.category_service
    }

fun mapListingEditorError(error: ListingEditorError): UiText {
    return when (error) {
        ListingEditorError.TitleInvalid -> UiText.ResourceString(Res.string.listing_editor_title_invalid)
        ListingEditorError.PriceRequired -> UiText.ResourceString(Res.string.listing_editor_price_required)
        ListingEditorError.AuthenticationRequired -> UiText.ResourceString(Res.string.error_authentication_required)
        ListingEditorError.ListingExists -> UiText.ResourceString(Res.string.error_listing_exists)
        ListingEditorError.NotFound -> UiText.ResourceString(Res.string.error_listing_not_found)
        ListingEditorError.Forbidden -> UiText.ResourceString(Res.string.error_operation_forbidden)
        is ListingEditorError.Generic -> UiText.VerbatimString(error.message)
    }
}

fun ListingEditorState.toNewListing(publisher: PublicProfile): NewListing? {
    val type = this.type ?: return null
    val price = Price(this.priceAmount, this.priceCurrency.code)
    val imageLinks = if (this.imageUrl.value.isNotBlank()) listOf(this.imageUrl) else emptyList()

    val commonData = CommonListingData(
        title = this.title,
        description = if (this.description.value.isBlank()) null else this.description,
        price = price,
        imageLinks = imageLinks,
        location = ListingLocation("Unknown")
    )

    return when (type) {
        ListingCategory.Item -> NewListing.Item(commonData, publisher)
        ListingCategory.Event -> {
            if (!this.isScheduleStepValid || this.scheduleBounds.startDate == null || this.scheduleBounds.startTime == null || this.scheduleBounds.endDate == null || this.scheduleBounds.endTime == null) return null

            val timeZone = TimeZone.of(this.scheduleBounds.timeZoneIdentifier.value)
            val startLocal = LocalDateTime(this.scheduleBounds.startDate, this.scheduleBounds.startTime)
            val endLocal = LocalDateTime(this.scheduleBounds.endDate, this.scheduleBounds.endTime)

            val schedule = EventSchedule(
                startLocalTime = startLocal,
                endLocalTime = endLocal,
                startInstant = startLocal.toInstant(timeZone),
                endInstant = endLocal.toInstant(timeZone),
                timeZoneIdentifier = this.scheduleBounds.timeZoneIdentifier
            )

            NewListing.Event(
                commonData = commonData,
                publisher = publisher,
                schedule = schedule,
                autoRsvp = true
            )
        }

        ListingCategory.Service -> NewListing.Service(commonData, publisher)
    }
}

fun ListingEditorState.toPatchRequest(): TypeV1ListingDtoPatchRequest? {
    val type = this.type ?: return null
    val imageLinks = if (this.imageUrl.value.isNotBlank()) listOf(this.imageUrl) else emptyList()
    val descriptionValue = if (this.description.value.isBlank()) null else this.description

    val priceObj = Price(this.priceAmount, this.priceCurrency.code)

    return when (type) {
        ListingCategory.Item -> ListingItemDtoSchema.V1.PatchRequest(
            title = Patchable.Set(this.title),
            description = Patchable.Set(descriptionValue),
            price = Patchable.Set(priceObj),
            imageLinks = Patchable.Set(imageLinks)
        )

        ListingCategory.Event -> {
            val schedulePatch =
                if (this.scheduleBounds.startDate != null && this.scheduleBounds.startTime != null && this.scheduleBounds.endDate != null && this.scheduleBounds.endTime != null && this.isScheduleStepValid) {
                    val timeZone = TimeZone.of(this.scheduleBounds.timeZoneIdentifier.value)
                    val startLocal = LocalDateTime(this.scheduleBounds.startDate, this.scheduleBounds.startTime)
                    val endLocal = LocalDateTime(this.scheduleBounds.endDate, this.scheduleBounds.endTime)
                    Patchable.Set(
                        EventScheduleDtoSchema.V1.PatchRequest(
                            startLocalTime = Patchable.Set(startLocal),
                            endLocalTime = Patchable.Set(endLocal),
                            startInstant = Patchable.Set(startLocal.toInstant(timeZone)),
                            endInstant = Patchable.Set(endLocal.toInstant(timeZone)),
                            timeZoneIdentifier = Patchable.Set(this.scheduleBounds.timeZoneIdentifier)
                        )
                    )
                } else {
                    Patchable.Unchanged
                }

            ListingEventDtoSchema.V1.PatchRequest(
                title = Patchable.Set(this.title),
                description = Patchable.Set(descriptionValue),
                price = Patchable.Set(priceObj),
                imageLinks = Patchable.Set(imageLinks),
                schedule = schedulePatch
            )
        }

        ListingCategory.Service -> ListingServiceDtoSchema.V1.PatchRequest(
            title = Patchable.Set(this.title),
            description = Patchable.Set(descriptionValue),
            price = Patchable.Set(priceObj),
            imageLinks = Patchable.Set(imageLinks)
        )
    }
}

fun ListingEditorState.toDraft(stepIndex: Int): ListingDraft? {
    val type = this.type ?: return null
    return ListingDraft(
        title = this.title,
        description = this.description,
        priceAmount = this.priceAmount,
        priceCurrencyCode = this.priceCurrency.code,
        imageUrl = this.imageUrl,
        startDate = this.scheduleBounds.startDate,
        startTime = this.scheduleBounds.startTime,
        endDate = this.scheduleBounds.endDate,
        endTime = this.scheduleBounds.endTime,
        timeZoneIdentifier = this.scheduleBounds.timeZoneIdentifier,
        type = type,
        stepIndex = stepIndex
    )
}

fun Listing.toEditorState(): ListingEditorState {
    val commonData = this.commonData
    val category = when (this) {
        is Listing.Item -> ListingCategory.Item
        is Listing.Event -> ListingCategory.Event
        is Listing.Service -> ListingCategory.Service
    }
    val schedule = (this as? Listing.Event)?.schedule

    val currency = SupportedCurrency.fromCode(commonData.price.currency) ?: SupportedCurrency.USD

    return ListingEditorState(
        isEditing = true,
        title = commonData.title,
        description = commonData.description ?: ListingDescription(""),
        priceAmount = commonData.price.amount,
        priceCurrency = currency,
        imageUrl = commonData.imageLinks.firstOrNull() ?: ImageUrl(""),
        scheduleBounds = DraftEventBounds(
            startDate = schedule?.startLocalTime?.date,
            startTime = schedule?.startLocalTime?.time,
            endDate = schedule?.endLocalTime?.date,
            endTime = schedule?.endLocalTime?.time,
            timeZoneIdentifier = schedule?.timeZoneIdentifier ?: TimeZoneIdentifier(TimeZone.currentSystemDefault().id)
        ),
        type = category,
        error = null,
        isSubmitting = false,
        showDiscardDraftDialog = false,
        showPublishConfirmationDialog = false
    )
}