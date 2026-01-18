package com.availelabs.availe.listingeditor

import kotlinx.serialization.Serializable

sealed interface ListingEditorPage {
    data object Type : ListingEditorPage
    data object Basics : ListingEditorPage
    data object Pricing : ListingEditorPage
    data object Media : ListingEditorPage
    data object Review : ListingEditorPage
}

@Serializable
sealed interface ListingEditorPageConfig {
    @Serializable
    data object Type : ListingEditorPageConfig

    @Serializable
    data object Basics : ListingEditorPageConfig

    @Serializable
    data object Pricing : ListingEditorPageConfig

    @Serializable
    data object Media : ListingEditorPageConfig

    @Serializable
    data object Review : ListingEditorPageConfig
}