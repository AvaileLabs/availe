package com.availelabs.availe.ui.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.availelabs.availe.ui.model.ActionIcon
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.Lucide

object ActionIconMapper {
    @Composable
    fun map(icon: ActionIcon): ImageVector {
        return when (icon) {
            ActionIcon.ADD -> Lucide.CirclePlus
        }
    }
}