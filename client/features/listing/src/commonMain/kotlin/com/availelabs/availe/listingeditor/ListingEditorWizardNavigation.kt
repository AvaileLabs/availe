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
import com.arkivanov.decompose.router.pages.ChildPages
import com.availelabs.availe.ListingEditorState
import com.availelabs.availe.domain.extensions.isValid
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
    pages: ChildPages<ListingEditorPageConfig, ListingEditorPage>,
    editorState: ListingEditorState,
    onStepClicked: (Int) -> Unit,
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
                    title = getStepTitle(config),
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
    Locked,
    Current,
    Complete,
    Invalid
}

internal fun buildStepUiState(
    index: Int,
    selectedIndex: Int,
    allConfigs: List<ListingEditorPageConfig>,
    editorState: ListingEditorState
): StepUiState {
    val isSelected = index == selectedIndex
    val config = allConfigs[index]

    val isValid = isStepValid(config, editorState)

    val isEnabled = (0 until index).all { i ->
        isStepValid(allConfigs[i], editorState)
    }

    val status = when {
        isSelected -> StepStatus.Current
        !isEnabled -> StepStatus.Locked
        isValid -> StepStatus.Complete
        else -> StepStatus.Invalid
    }

    return StepUiState(
        isSelected = isSelected,
        isEnabled = isEnabled,
        status = status
    )
}

internal fun isStepValid(config: ListingEditorPageConfig, state: ListingEditorState): Boolean {
    return when (config) {
        ListingEditorPageConfig.Type -> true
        ListingEditorPageConfig.Basics -> state.title.isValid()
        ListingEditorPageConfig.Pricing -> state.priceAmount.isValid() && state.priceAmount.value.isNotBlank()
        ListingEditorPageConfig.Media -> true
        ListingEditorPageConfig.Review -> {
            isStepValid(ListingEditorPageConfig.Basics, state) &&
                    isStepValid(ListingEditorPageConfig.Pricing, state)
        }
    }
}

internal fun getStepTitle(config: ListingEditorPageConfig): String {
    return when (config) {
        ListingEditorPageConfig.Type -> "Type"
        ListingEditorPageConfig.Basics -> "Basics"
        ListingEditorPageConfig.Pricing -> "Pricing"
        ListingEditorPageConfig.Media -> "Media"
        ListingEditorPageConfig.Review -> "Review"
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
        status == StepStatus.Invalid -> Theme[colors][destructive].copy(alpha = 0.5f)
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
            color = if (status == StepStatus.Locked) textColor.copy(alpha = 0.5f) else textColor,
            fontWeight = fontWeight,
            modifier = Modifier.weight(1f)
        )

        when (status) {
            StepStatus.Complete -> {
                Icon(
                    imageVector = Lucide.CircleCheck,
                    contentDescription = "Complete",
                    tint = successColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            StepStatus.Invalid -> {
                Icon(
                    imageVector = Lucide.CircleAlert,
                    contentDescription = "Invalid",
                    tint = errorColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            else -> Unit
        }

        if (status != StepStatus.Current && status != StepStatus.Locked) {
            Spacer(Modifier.width(12.dp))
        }
    }
}