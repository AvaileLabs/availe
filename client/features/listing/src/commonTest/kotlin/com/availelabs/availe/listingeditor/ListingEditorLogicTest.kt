package com.availelabs.availe.listingeditor

import com.availelabs.availe.ListingEditorError
import com.availelabs.availe.ListingEditorIntent
import com.availelabs.availe.ListingEditorState
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.ListingTitle
import com.availelabs.availe.domain.model.PriceAmount
import com.availelabs.availe.reduceListingEditor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListingEditorLogicTest {

    @Test
    fun changeTitleRecalculatesValidation() {
        val initialState = ListingEditorState()
        val intent = ListingEditorIntent.ChangeTitle(ListingTitle("Valid Title"))
        val newState = reduceListingEditor(initialState, intent)

        assertTrue(newState.isBasicsStepValid)
        assertEquals("Valid Title", newState.title.value)
    }

    @Test
    fun changePriceRecalculatesValidation() {
        val initialState = ListingEditorState()
        val intent = ListingEditorIntent.ChangePrice(PriceAmount("10.00"))
        val newState = reduceListingEditor(initialState, intent)

        assertTrue(newState.isPricingStepValid)
        assertEquals("10.00", newState.priceAmount.value)
    }

    @Test
    fun askToPublishWithInvalidTitleEmitsError() {
        val initialState = ListingEditorState(
            title = ListingTitle(""),
            isBasicsStepValid = false
        )
        val intent = ListingEditorIntent.AskToPublish
        val newState = reduceListingEditor(initialState, intent)

        assertEquals(ListingEditorError.TitleInvalid, newState.error)
        assertFalse(newState.showPublishConfirmationDialog)
    }

    @Test
    fun askToPublishWithInvalidPriceEmitsError() {
        val initialState = ListingEditorState(
            title = ListingTitle("Valid Title"),
            isBasicsStepValid = true,
            priceAmount = PriceAmount(""),
            isPricingStepValid = false
        )
        val intent = ListingEditorIntent.AskToPublish
        val newState = reduceListingEditor(initialState, intent)

        assertEquals(ListingEditorError.PriceRequired, newState.error)
        assertFalse(newState.showPublishConfirmationDialog)
    }

    @Test
    fun askToPublishWithValidStateShowsConfirmationDialog() {
        val initialState = ListingEditorState(
            title = ListingTitle("Valid Title"),
            isBasicsStepValid = true,
            priceAmount = PriceAmount("10.00"),
            isPricingStepValid = true
        )
        val intent = ListingEditorIntent.AskToPublish
        val newState = reduceListingEditor(initialState, intent)

        assertTrue(newState.showPublishConfirmationDialog)
        assertNull(newState.error)
    }

    @Test
    fun confirmDiscardDraftResetsStateAndPreservesTypeIfEditing() {
        val initialState = ListingEditorState(
            isEditing = true,
            type = ListingCategory.Item,
            title = ListingTitle("Draft Title"),
            priceAmount = PriceAmount("20.00")
        )
        val intent = ListingEditorIntent.ConfirmDiscardDraft
        val newState = reduceListingEditor(initialState, intent)

        assertEquals("", newState.title.value)
        assertEquals("", newState.priceAmount.value)
        assertEquals(ListingCategory.Item, newState.type)
        assertFalse(newState.isBasicsStepValid)
    }

    @Test
    fun clearCurrentStepNullifiesSpecificPageData() {
        val initialState = ListingEditorState(
            title = ListingTitle("Some Title")
        )
        val intent = ListingEditorIntent.ClearCurrentStep(ListingEditorPageConfig.Basics)
        val newState = reduceListingEditor(initialState, intent)

        assertEquals("", newState.title.value)
        assertEquals("", newState.description.value)
        assertFalse(newState.isBasicsStepValid)
    }
}