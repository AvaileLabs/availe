package com.availelabs.availe.listingeditor

import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.category_event
import availe.client.designsystem.generated.resources.category_item
import availe.client.designsystem.generated.resources.category_service
import availe.client.designsystem.generated.resources.listing_editor_price_required
import availe.client.designsystem.generated.resources.listing_editor_step_basics
import availe.client.designsystem.generated.resources.listing_editor_step_media
import availe.client.designsystem.generated.resources.listing_editor_step_pricing
import availe.client.designsystem.generated.resources.listing_editor_step_review
import availe.client.designsystem.generated.resources.listing_editor_step_type
import availe.client.designsystem.generated.resources.listing_editor_title_invalid
import com.availelabs.availe.ListingEditorError
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.ui.model.UiText
import org.jetbrains.compose.resources.StringResource

val ListingEditorPageConfig.titleRes: StringResource
    get() = when (this) {
        ListingEditorPageConfig.Type -> Res.string.listing_editor_step_type
        ListingEditorPageConfig.Basics -> Res.string.listing_editor_step_basics
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
        is ListingEditorError.Generic -> UiText.VerbatimString(error.message)
    }
}