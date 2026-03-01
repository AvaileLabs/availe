package com.availelabs.availe.listingeditor

import availe.client.designsystem.generated.resources.*
import com.availelabs.availe.ListingEditorError
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.ui.model.UiText
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