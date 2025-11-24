package com.availelabs.availe.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.availelabs.availe.ui.modifiers.refreshTrigger
import com.composables.uikit.components.IntermediateProgressIndicator
import com.composables.uikit.components.PrimaryButton
import com.composables.uikit.components.Text
import com.composables.uikit.styling.background
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.onBackground
import com.composeunstyled.theme.Theme

@Composable
fun <T> RefreshableScreenScaffold(
    state: ContentState<T>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    appBar: (@Composable (RefreshUiState) -> Unit)? = null,
    sideBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    backgroundColor: Color = Theme[colors][background],
    contentColor: Color = Theme[colors][onBackground],
    content: @Composable BoxScope.(T, RefreshUiState) -> Unit,
) {
    val refreshStateHolder = remember { RefreshStateHolder() }

    val refreshUiState by remember(state, refreshStateHolder) {
        derivedStateOf {
            RefreshUiState(
                isRefreshing = state is ContentState.Refreshing || state is ContentState.Loading,
                pullProgress = refreshStateHolder.pullProgress
            )
        }
    }

    ScreenScaffold(
        modifier = modifier,
        appBar = if (appBar != null) {
            { appBar(refreshUiState) }
        } else null,
        sideBar = sideBar,
        bottomBar = bottomBar,
        backgroundColor = backgroundColor,
        contentColor = contentColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .refreshTrigger(
                    state = refreshStateHolder,
                    onRefresh = onRefresh
                )
        ) {
            when (state) {
                is ContentState.Idle -> {
                }

                is ContentState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        IntermediateProgressIndicator()
                    }
                }

                is ContentState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message)
                        Spacer(modifier = Modifier.height(16.dp))
                        PrimaryButton(onClick = onRefresh) {
                            Text("Retry")
                        }
                    }
                }

                is ContentState.Content -> {
                    content(state.data, refreshUiState)
                }

                is ContentState.Refreshing -> {
                    content(state.data, refreshUiState)
                }
            }
        }
    }
}