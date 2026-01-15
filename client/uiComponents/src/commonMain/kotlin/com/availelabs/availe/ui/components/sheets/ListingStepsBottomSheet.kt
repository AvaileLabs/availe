package com.availelabs.availe.ui.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.CircleDot
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.Icon
import com.composables.uikit.components.Text
import com.composables.uikit.styling.colors
import com.composables.uikit.styling.primary
import com.composeunstyled.theme.Theme

@Composable
fun ListingStepsBottomSheet(
    currentStepIndex: Int,
    steps: List<String>,
    onStepSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme[colors][primary], RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(vertical = 24.dp)
    ) {
        Text(
            text = "Listing Steps",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )

        steps.forEachIndexed { index, stepName ->
            val isCompleted = index < currentStepIndex
            val isCurrent = index == currentStepIndex

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStepSelected(index) }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when {
                    isCompleted -> Lucide.Check
                    isCurrent -> Lucide.CircleDot
                    else -> Lucide.Circle
                }

                val tint =
                    if (isCompleted || isCurrent) Theme[colors][primary] else Theme[colors][primary].copy(alpha = 0.3f)

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = stepName,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) Theme[colors][primary] else Theme[colors][primary].copy(alpha = 0.7f)
                )
            }
        }
    }
}