package com.availelabs.availe.listingeditor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.router.pages.ChildPages
import com.availelabs.availe.ListingEditorState
import com.composables.uikit.components.Text
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.onCard
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.theme.Theme

@Composable
internal fun ListingStepsBottomSheet(
    pages: ChildPages<ListingEditorPageConfig, ListingEditorPage>,
    editorState: ListingEditorState,
    onStepSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        ProvideContentColor(Theme[colors][onCard]) {
            Text(
                text = "Listing Steps",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

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
                    onClick = { onStepSelected(index) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}