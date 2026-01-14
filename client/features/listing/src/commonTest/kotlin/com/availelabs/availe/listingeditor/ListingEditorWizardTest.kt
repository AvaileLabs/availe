package com.availelabs.availe.listingeditor

import com.availelabs.availe.ListingEditorIntent
import com.availelabs.availe.ListingEditorState
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.ListingTitle
import com.availelabs.availe.domain.model.PriceAmount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListingEditorWizardTest {

    @Test
    fun createFlowStartsAtChooseType() {
        val wizardState = ListingEditorWizardState.initial(isEditingFlow = false)

        assertEquals(ListingEditorWizardStep.ChooseType, wizardState.currentStep)
        assertNull(wizardState.selectedCategory)
        assertTrue(ListingEditorWizardStep.ChooseType in wizardState.steps)
    }

    @Test
    fun nextFromChooseTypeWithoutSelectionSetsError() {
        val wizardState = ListingEditorWizardState.initial(isEditingFlow = false)
        val reduction = reduceListingEditorWizard(
            wizardState = wizardState,
            editorState = ListingEditorState(),
            intent = ListingEditorWizardIntent.Next
        )

        assertEquals(ListingEditorWizardStep.ChooseType, reduction.state.currentStep)
        assertEquals(ListingEditorIntent.SetError("Please choose a type"), reduction.editorIntent)
    }

    @Test
    fun selectingTypeAdvancesToBasicsAndDispatchesChangeType() {
        val wizardState = ListingEditorWizardState.initial(isEditingFlow = false)
        val reduction = reduceListingEditorWizard(
            wizardState = wizardState,
            editorState = ListingEditorState(),
            intent = ListingEditorWizardIntent.SelectType(ListingCategory.Event)
        )

        assertEquals(ListingCategory.Event, reduction.state.selectedCategory)
        assertEquals(ListingEditorWizardStep.Basics, reduction.state.currentStep)
        assertEquals(ListingEditorIntent.ChangeType(ListingCategory.Event), reduction.editorIntent)
    }

    @Test
    fun basicsNextRequiresValidTitle() {
        val wizardState = ListingEditorWizardState.initial(isEditingFlow = false).copy(
            selectedCategory = ListingCategory.Item,
            currentStep = ListingEditorWizardStep.Basics
        )

        val reduction = reduceListingEditorWizard(
            wizardState = wizardState,
            editorState = ListingEditorState(title = ListingTitle("")),
            intent = ListingEditorWizardIntent.Next
        )

        assertEquals(ListingEditorWizardStep.Basics, reduction.state.currentStep)
        assertEquals(ListingEditorIntent.SetError("Title is invalid"), reduction.editorIntent)
    }

    @Test
    fun basicsNextAdvancesWhenTitleValid() {
        val wizardState = ListingEditorWizardState.initial(isEditingFlow = false).copy(
            selectedCategory = ListingCategory.Item,
            currentStep = ListingEditorWizardStep.Basics
        )

        val reduction = reduceListingEditorWizard(
            wizardState = wizardState,
            editorState = ListingEditorState(title = ListingTitle("Valid title")),
            intent = ListingEditorWizardIntent.Next
        )

        assertEquals(ListingEditorWizardStep.Pricing, reduction.state.currentStep)
        assertNull(reduction.editorIntent)
    }

    @Test
    fun pricingNextRequiresPrice() {
        val wizardState = ListingEditorWizardState.initial(isEditingFlow = false).copy(
            selectedCategory = ListingCategory.Item,
            currentStep = ListingEditorWizardStep.Pricing
        )

        val reduction = reduceListingEditorWizard(
            wizardState = wizardState,
            editorState = ListingEditorState(priceAmount = PriceAmount("")),
            intent = ListingEditorWizardIntent.Next
        )

        assertEquals(ListingEditorWizardStep.Pricing, reduction.state.currentStep)
        assertEquals(ListingEditorIntent.SetError("Price is required"), reduction.editorIntent)
    }

    @Test
    fun reviewNextSubmits() {
        val wizardState = ListingEditorWizardState.initial(isEditingFlow = false).copy(
            selectedCategory = ListingCategory.Item,
            currentStep = ListingEditorWizardStep.Review
        )

        val editorState = ListingEditorState(
            title = ListingTitle("Valid title"),
            priceAmount = PriceAmount("10")
        )

        val reduction = reduceListingEditorWizard(
            wizardState = wizardState,
            editorState = editorState,
            intent = ListingEditorWizardIntent.Next
        )

        assertEquals(ListingEditorIntent.Submit, reduction.editorIntent)
        assertEquals(ListingEditorWizardStep.Review, reduction.state.currentStep)
    }

    @Test
    fun editFlowHasNoChooseTypeStep() {
        val wizardState = ListingEditorWizardState.initial(isEditingFlow = true)

        assertEquals(ListingEditorWizardStep.Basics, wizardState.currentStep)
        assertTrue(ListingEditorWizardStep.ChooseType !in wizardState.steps)
    }
}