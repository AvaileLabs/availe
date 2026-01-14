package com.availelabs.availe.listingeditor

import com.availelabs.availe.ListingEditorIntent
import com.availelabs.availe.ListingEditorState
import com.availelabs.availe.domain.extensions.isValid
import com.availelabs.availe.domain.model.ListingCategory

sealed interface ListingEditorWizardStep {
    val title: String

    data object ChooseType : ListingEditorWizardStep {
        override val title: String = "Choose type"
    }

    data object Basics : ListingEditorWizardStep {
        override val title: String = "Basics"
    }

    data object Pricing : ListingEditorWizardStep {
        override val title: String = "Pricing"
    }

    data object Media : ListingEditorWizardStep {
        override val title: String = "Media"
    }

    data object Review : ListingEditorWizardStep {
        override val title: String = "Review"
    }
}

enum class ListingEditorStepStatus {
    Locked,
    Pending,
    Valid,
    Complete,
    Invalid
}

data class ListingEditorWizardState(
    val isEditingFlow: Boolean,
    val selectedCategory: ListingCategory?,
    val steps: List<ListingEditorWizardStep>,
    val currentStep: ListingEditorWizardStep
) {
    val currentStepIndex: Int
        get() = steps.indexOf(currentStep).coerceAtLeast(0)

    companion object {
        fun initial(isEditingFlow: Boolean): ListingEditorWizardState {
            val steps = if (isEditingFlow) {
                listOf(
                    ListingEditorWizardStep.Basics,
                    ListingEditorWizardStep.Pricing,
                    ListingEditorWizardStep.Media,
                    ListingEditorWizardStep.Review
                )
            } else {
                listOf(
                    ListingEditorWizardStep.ChooseType,
                    ListingEditorWizardStep.Basics,
                    ListingEditorWizardStep.Pricing,
                    ListingEditorWizardStep.Media,
                    ListingEditorWizardStep.Review
                )
            }

            return ListingEditorWizardState(
                isEditingFlow = isEditingFlow,
                selectedCategory = if (isEditingFlow) ListingCategory.Item else null,
                steps = steps,
                currentStep = steps.first()
            )
        }
    }
}

sealed interface ListingEditorWizardIntent {
    data class SelectType(val category: ListingCategory) : ListingEditorWizardIntent
    data object Next : ListingEditorWizardIntent
    data object Back : ListingEditorWizardIntent
    data class NavigateTo(val step: ListingEditorWizardStep) : ListingEditorWizardIntent
}

data class ListingEditorWizardReduction(
    val state: ListingEditorWizardState,
    val editorIntent: ListingEditorIntent?
)

fun reduceListingEditorWizard(
    wizardState: ListingEditorWizardState,
    editorState: ListingEditorState,
    intent: ListingEditorWizardIntent
): ListingEditorWizardReduction {
    return when (intent) {
        is ListingEditorWizardIntent.SelectType -> {
            if (wizardState.isEditingFlow) {
                ListingEditorWizardReduction(wizardState, null)
            } else {
                ListingEditorWizardReduction(
                    state = wizardState.copy(
                        selectedCategory = intent.category,
                        currentStep = wizardState.currentStep
                    ),
                    editorIntent = ListingEditorIntent.ChangeType(intent.category)
                )
            }
        }

        ListingEditorWizardIntent.Back -> {
            val currentIndex = wizardState.currentStepIndex
            if (currentIndex <= 0) {
                ListingEditorWizardReduction(wizardState, null)
            } else {
                ListingEditorWizardReduction(
                    state = wizardState.copy(currentStep = wizardState.steps[currentIndex - 1]),
                    editorIntent = null
                )
            }
        }

        ListingEditorWizardIntent.Next -> {
            val currentStep = wizardState.currentStep
            if (currentStep == ListingEditorWizardStep.Review) {
                ListingEditorWizardReduction(wizardState, ListingEditorIntent.Submit)
            } else {
                val errorMessage = getBlockingStepErrorMessage(wizardState, editorState)
                if (errorMessage != null) {
                    ListingEditorWizardReduction(wizardState, ListingEditorIntent.SetError(errorMessage))
                } else {
                    val nextIndex = wizardState.currentStepIndex + 1
                    if (nextIndex !in wizardState.steps.indices) {
                        ListingEditorWizardReduction(wizardState, null)
                    } else {
                        ListingEditorWizardReduction(
                            state = wizardState.copy(currentStep = wizardState.steps[nextIndex]),
                            editorIntent = null
                        )
                    }
                }
            }
        }

        is ListingEditorWizardIntent.NavigateTo -> {
            if (intent.step !in wizardState.steps) {
                ListingEditorWizardReduction(wizardState, null)
            } else if (!wizardState.isEditingFlow && wizardState.selectedCategory == null && intent.step != ListingEditorWizardStep.ChooseType) {
                ListingEditorWizardReduction(wizardState, null)
            } else {
                ListingEditorWizardReduction(
                    state = wizardState.copy(currentStep = intent.step),
                    editorIntent = null
                )
            }
        }
    }
}

fun getBlockingStepErrorMessage(
    wizardState: ListingEditorWizardState,
    editorState: ListingEditorState
): String? {
    return when (wizardState.currentStep) {
        ListingEditorWizardStep.ChooseType -> {
            if (wizardState.selectedCategory == null) "Please choose a type" else null
        }

        ListingEditorWizardStep.Basics -> {
            if (!editorState.title.isValid()) "Title is invalid" else null
        }

        ListingEditorWizardStep.Pricing -> {
            if (editorState.priceAmount.value.isBlank()) "Price is required" else null
        }

        ListingEditorWizardStep.Media -> null // Images are optional

        ListingEditorWizardStep.Review -> {
            if (!wizardState.isEditingFlow && wizardState.selectedCategory == null) {
                "Please choose a type"
            } else if (!editorState.title.isValid()) {
                "Title is invalid"
            } else if (editorState.priceAmount.value.isBlank()) {
                "Price is required"
            } else {
                null
            }
        }
    }
}

fun getListingEditorStepStatus(
    step: ListingEditorWizardStep,
    wizardState: ListingEditorWizardState,
    editorState: ListingEditorState
): ListingEditorStepStatus {
    if (!wizardState.isEditingFlow && wizardState.selectedCategory == null && step != ListingEditorWizardStep.ChooseType) {
        return ListingEditorStepStatus.Locked
    }

    return when (step) {
        ListingEditorWizardStep.ChooseType -> {
            if (wizardState.selectedCategory != null) ListingEditorStepStatus.Complete else ListingEditorStepStatus.Pending
        }

        ListingEditorWizardStep.Basics -> {
            if (!editorState.title.isValid()) {
                if (editorState.title.value.isNotEmpty()) ListingEditorStepStatus.Invalid else ListingEditorStepStatus.Pending
            } else {
                if (editorState.description.value.isNotBlank()) ListingEditorStepStatus.Complete else ListingEditorStepStatus.Valid
            }
        }

        ListingEditorWizardStep.Pricing -> {
            if (editorState.priceAmount.value.isBlank()) {
                ListingEditorStepStatus.Pending
            } else if (!editorState.priceAmount.isValid()) {
                ListingEditorStepStatus.Invalid
            } else {
                ListingEditorStepStatus.Complete
            }
        }

        ListingEditorWizardStep.Media -> {
            if (editorState.imageUrl.value.isBlank()) {
                ListingEditorStepStatus.Valid // Empty is valid (optional)
            } else if (!editorState.imageUrl.isValid()) {
                ListingEditorStepStatus.Invalid
            } else {
                ListingEditorStepStatus.Complete // Has valid image -> Complete
            }
        }

        ListingEditorWizardStep.Review -> {
            val isReady = isListingEditorWizardReadyForReview(wizardState, editorState)
            if (isReady) {
                // If everything is not just "Valid" but "Complete" (i.e. has description and images), then Review is Complete.
                val isFullyComplete =
                    editorState.description.value.isNotBlank() && editorState.imageUrl.value.isNotBlank()
                if (isFullyComplete) ListingEditorStepStatus.Complete else ListingEditorStepStatus.Valid
            } else {
                ListingEditorStepStatus.Locked
            }
        }
    }
}

private fun isListingEditorWizardReadyForReview(
    wizardState: ListingEditorWizardState,
    editorState: ListingEditorState
): Boolean {
    return (wizardState.isEditingFlow || wizardState.selectedCategory != null) &&
            editorState.title.isValid() &&
            editorState.priceAmount.value.isNotBlank()
    // Image is optional, so not checked here for readiness
}