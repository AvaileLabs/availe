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
import availe.client.designsystem.generated.resources.*
import com.arkivanov.decompose.router.pages.ChildPages
import com.availelabs.availe.ListingEditorState
import com.availelabs.availe.domain.model.ScheduleValidationState
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.Card
import com.composables.uikit.components.Icon
import com.composables.uikit.styling.*
import com.composeunstyled.UnstyledText
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ListingEditorWizardStepList(
    pages: ChildPages<ListingEditorPageConfig, ListingEditorPage>,
    editorState: ListingEditorState,
    onStepClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        title = {
            SelectionContainer {
                UnstyledText(stringResource(Res.string.listing_editor_steps_title))
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val stepConfigs = pages.items.map { it.configuration }
            stepConfigs.forEachIndexed { index, config ->
                val uiState = remember(index, pages.selectedIndex, editorState) {
                    buildStepUiState(
                        index = index,
                        selectedIndex = pages.selectedIndex,
                        allConfigs = stepConfigs,
                        editorState = editorState
                    )
                }

                ListingStepRow(
                    title = stringResource(config.titleRes),
                    isSelected = uiState.isSelected,
                    isEnabled = uiState.isEnabled,
                    status = uiState.status,
                    onClick = { onStepClicked(index) }
                )
            }
        }
    }
}

@Composable
internal fun ListingEditorSegmentedProgressBar(
    pages: ChildPages<ListingEditorPageConfig, ListingEditorPage>,
    editorState: ListingEditorState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val stepConfigs = pages.items.map { it.configuration }
        stepConfigs.forEachIndexed { index, config ->
            val uiState = remember(index, pages.selectedIndex, editorState) {
                buildStepUiState(
                    index = index,
                    selectedIndex = pages.selectedIndex,
                    allConfigs = stepConfigs,
                    editorState = editorState
                )
            }

            ListingEditorProgressBarSegment(
                isSelected = uiState.isSelected,
                status = uiState.status,
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
            )
        }
    }
}

internal data class StepUiState(
    val isSelected: Boolean,
    val isEnabled: Boolean,
    val status: StepStatus
)

internal enum class StepStatus {
    Incomplete,
    Satisfied,
    Complete
}

internal fun buildStepUiState(
    index: Int,
    selectedIndex: Int,
    allConfigs: List<ListingEditorPageConfig>,
    editorState: ListingEditorState
): StepUiState {
    val isSelected = index == selectedIndex
    val config = allConfigs[index]

    val status = getStepStatus(config, editorState)
    val isEnabled = true

    return StepUiState(
        isSelected = isSelected,
        isEnabled = isEnabled,
        status = status
    )
}

internal fun getStepStatus(config: ListingEditorPageConfig, state: ListingEditorState): StepStatus {
    return when (config) {
        ListingEditorPageConfig.Type -> {
            if (state.type != null) StepStatus.Complete else StepStatus.Incomplete
        }

        ListingEditorPageConfig.Basics -> {
            if (!state.isBasicsStepValid) {
                StepStatus.Incomplete
            } else if (state.description.value.isBlank()) {
                StepStatus.Satisfied
            } else {
                StepStatus.Complete
            }
        }

        ListingEditorPageConfig.Schedule -> {
            if (state.scheduleValidationState is ScheduleValidationState.Valid) {
                StepStatus.Complete
            } else {
                StepStatus.Incomplete
            }
        }

        ListingEditorPageConfig.Pricing -> {
            if (state.isPricingStepValid) {
                StepStatus.Complete
            } else {
                StepStatus.Incomplete
            }
        }

        ListingEditorPageConfig.Media -> {
            if (state.imageUrl.value.isBlank()) {
                StepStatus.Incomplete
            } else {
                StepStatus.Complete
            }
        }

        ListingEditorPageConfig.Review -> {
            if (state.isBasicsStepValid && state.isPricingStepValid && state.scheduleValidationState is ScheduleValidationState.Valid && state.type != null) {
                StepStatus.Complete
            } else {
                StepStatus.Incomplete
            }
        }
    }
}

internal fun getStepValidationStatus(config: ListingEditorPageConfig, state: ListingEditorState): Boolean {
    return when (config) {
        ListingEditorPageConfig.Type -> state.type != null
        ListingEditorPageConfig.Basics -> state.isBasicsStepValid
        ListingEditorPageConfig.Schedule -> state.scheduleValidationState is ScheduleValidationState.Valid
        ListingEditorPageConfig.Pricing -> state.isPricingStepValid
        ListingEditorPageConfig.Media -> true
        ListingEditorPageConfig.Review -> state.isBasicsStepValid && state.isPricingStepValid && state.scheduleValidationState is ScheduleValidationState.Valid && state.type != null
    }
}

@Composable
private fun ListingEditorProgressBarSegment(
    isSelected: Boolean,
    status: StepStatus,
    modifier: Modifier = Modifier
) {
    val color = when {
        isSelected -> Theme[colors][primary]
        status == StepStatus.Complete -> Theme[colors][primary].copy(alpha = 0.5f)
        status == StepStatus.Satisfied -> Theme[colors][primary].copy(alpha = 0.5f)
        else -> Theme[colors][outline].copy(alpha = 0.2f)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
internal fun ListingStepRow(
    title: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    status: StepStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Theme[colors][outline].copy(alpha = 0.05f) else Color.Transparent
    val textColor = if (isSelected) Theme[colors][primary] else Theme[colors][onBackground]
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
    val successColor = Color(0xFF10B981)
    val pendingColor = Theme[colors][outline].copy(alpha = 0.3f)

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

        UnstyledText(
            text = title,
            style = Theme[textStyles][body],
            color = textColor,
            fontWeight = fontWeight,
            modifier = Modifier.weight(1f)
        )

        when (status) {
            StepStatus.Complete -> {
                Icon(
                    imageVector = Lucide.CircleCheck,
                    contentDescription = stringResource(Res.string.accessibility_step_complete),
                    tint = successColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            StepStatus.Satisfied -> {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = stringResource(Res.string.accessibility_step_satisfied),
                    tint = successColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            StepStatus.Incomplete -> {
                Icon(
                    imageVector = Lucide.Circle,
                    contentDescription = stringResource(Res.string.accessibility_step_incomplete),
                    tint = pendingColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))
    }
}