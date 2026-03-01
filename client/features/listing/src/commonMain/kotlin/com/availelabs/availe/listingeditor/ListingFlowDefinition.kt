package com.availelabs.availe.listingeditor

import com.availelabs.availe.domain.model.ListingCategory

object ListingFlowDefinition {
    fun getSteps(category: ListingCategory?): List<ListingEditorPageConfig> {
        return when (category) {
            null -> listOf(
                ListingEditorPageConfig.Type
            )

            ListingCategory.Item -> listOf(
                ListingEditorPageConfig.Type,
                ListingEditorPageConfig.Basics,
                ListingEditorPageConfig.Pricing,
                ListingEditorPageConfig.Media,
                ListingEditorPageConfig.Review
            )

            ListingCategory.Event -> listOf(
                ListingEditorPageConfig.Basics,
                ListingEditorPageConfig.Schedule,
                ListingEditorPageConfig.Pricing,
                ListingEditorPageConfig.Media,
                ListingEditorPageConfig.Review
            )

            ListingCategory.Service -> listOf(
                ListingEditorPageConfig.Type,
                ListingEditorPageConfig.Basics,
                ListingEditorPageConfig.Pricing,
                ListingEditorPageConfig.Media,
                ListingEditorPageConfig.Review
            )
        }
    }
}