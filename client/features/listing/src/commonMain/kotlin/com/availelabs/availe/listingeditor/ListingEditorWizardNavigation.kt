package com.availelabs.availe.listingeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.availelabs.availe.ListingEditorState
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.Card
import com.composables.uikit.components.Icon
import com.composables.uikit.styling.*
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme

@Composable
internal fun ListingEditorWizardStepList(
    editorState: ListingEditorState,
    wizardState: ListingEditorWizardState,
    onStepClicked: (ListingEditorWizardStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        title = {
            SelectionContainer {
                Text("Steps")
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            wizardState.steps.forEach { step ->
                val uiState = remember(step, wizardState, editorState) {
                    buildListingEditorWizardStepUiState(
                        step = step,
                        wizardState = wizardState,
                        editorState = editorState
                    )
                }

                ListingEditorWizardStepListItem(
                    title = step.title,
                    isSelected = uiState.isSelected,
                    isEnabled = uiState.isEnabled,
                    status = uiState.status,
                    onClick = { onStepClicked(step) }
                )
            }
        }
    }
}

@Composable
internal fun ListingEditorSegmentedProgressBar(
    editorState: ListingEditorState,
    wizardState: ListingEditorWizardState,
    onStepClicked: (ListingEditorWizardStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        wizardState.steps.forEach { step ->
            val uiState = buildListingEditorWizardStepUiState(
                step = step,
                wizardState = wizardState,
                editorState = editorState
            )

            ListingEditorProgressBarSegment(
                isSelected = uiState.isSelected,
                status = uiState.status,
                isEnabled = uiState.isEnabled,
                onClick = { onStepClicked(step) },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
            )
        }
    }
}

private data class ListingEditorWizardStepUiState(
    val isSelected: Boolean,
    val isEnabled: Boolean,
    val status: ListingEditorStepStatus
)

private fun buildListingEditorWizardStepUiState(
    step: ListingEditorWizardStep,
    wizardState: ListingEditorWizardState,
    editorState: ListingEditorState
): ListingEditorWizardStepUiState {
    val isSelected = step == wizardState.currentStep
    val isEnabled =
        wizardState.isEditingFlow || wizardState.selectedCategory != null || step == ListingEditorWizardStep.ChooseType
    val status = getListingEditorStepStatus(
        step = step,
        wizardState = wizardState,
        editorState = editorState
    )

    return ListingEditorWizardStepUiState(
        isSelected = isSelected,
        isEnabled = isEnabled,
        status = status
    )
}

@Composable
private fun ListingEditorProgressBarSegment(
    isSelected: Boolean,
    status: ListingEditorStepStatus,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = when {
        isSelected -> Theme[colors][primary]
        status == ListingEditorStepStatus.Complete || status == ListingEditorStepStatus.Valid -> Theme[colors][primary].copy(
            alpha = 0.5f
        )

        status == ListingEditorStepStatus.Invalid -> Theme[colors][destructive].copy(alpha = 0.5f)
        else -> Theme[colors][outline].copy(alpha = 0.2f)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color)
            .clickable(enabled = isEnabled) { onClick() }
    )
}

@Composable
private fun ListingEditorWizardStepListItem(
    title: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    status: ListingEditorStepStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Theme[colors][outline].copy(alpha = 0.05f) else Color.Transparent
    val textColor = if (isSelected) Theme[colors][primary] else Theme[colors][onBackground]
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
    val successColor = Color(0xFF10B981)
    val errorColor = Theme[colors][destructive]

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(Theme[shapes][small])
            .background(backgroundColor)
            .clickable(enabled = isEnabled) { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Theme[colors][primary])
            )
        } else {
            Spacer(Modifier.width(4.dp))
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = title,
            style = Theme[textStyles][body],
            color = if (status == ListingEditorStepStatus.Locked) textColor.copy(alpha = 0.5f) else textColor,
            fontWeight = fontWeight,
            modifier = Modifier.weight(1f)
        )

        when (status) {
            ListingEditorStepStatus.Complete -> {
                Icon(
                    imageVector = Lucide.CircleCheck,
                    contentDescription = "Complete",
                    tint = successColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            ListingEditorStepStatus.Valid -> {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = "Valid",
                    tint = successColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            ListingEditorStepStatus.Invalid -> {
                Icon(
                    imageVector = Lucide.CircleAlert,
                    contentDescription = "Invalid",
                    tint = errorColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            else -> Unit
        }

        if (status != ListingEditorStepStatus.Pending && status != ListingEditorStepStatus.Locked) {
            Spacer(Modifier.width(12.dp))
        }
    }
}