package com.availelabs.availe.ui.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import availe.client.designsystem.generated.resources.*
import com.availelabs.availe.ui.model.EventProximity
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Timer
import com.composables.uikit.styling.*
import com.composeunstyled.theme.Theme
import org.jetbrains.compose.resources.stringResource

data class ProximityUiConfiguration(
    val text: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val contentColor: Color
)

object EventProximityUiMapper {
    @Composable
    fun map(proximity: EventProximity): ProximityUiConfiguration {
        return when (proximity) {
            is EventProximity.Ongoing -> ProximityUiConfiguration(
                text = stringResource(Res.string.agenda_proximity_ongoing),
                icon = Lucide.Timer,
                backgroundColor = Theme[colors][accent],
                contentColor = Theme[colors][onAccent]
            )

            is EventProximity.Today -> ProximityUiConfiguration(
                text = stringResource(Res.string.agenda_proximity_today),
                icon = Lucide.Clock,
                backgroundColor = Theme[colors][accent],
                contentColor = Theme[colors][onAccent]
            )

            is EventProximity.Tomorrow -> ProximityUiConfiguration(
                text = stringResource(Res.string.agenda_proximity_tomorrow),
                icon = Lucide.Clock,
                backgroundColor = Theme[colors][secondary],
                contentColor = Theme[colors][onSecondary]
            )

            is EventProximity.Future -> ProximityUiConfiguration(
                text = stringResource(Res.string.agenda_proximity_future_format, proximity.daysAway),
                icon = Lucide.Clock,
                backgroundColor = Theme[colors][secondary],
                contentColor = Theme[colors][onSecondary]
            )
        }
    }
}