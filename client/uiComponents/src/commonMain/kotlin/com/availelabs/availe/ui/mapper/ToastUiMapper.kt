package com.availelabs.availe.ui.mapper

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.availelabs.availe.model.ToastId
import com.availelabs.availe.model.ToastMessage
import com.availelabs.availe.ui.model.UiText
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide

data class UiToastModel(
    val id: ToastId,
    val text: UiText,
    val icon: ImageVector,
    val iconColor: Color?,
    val isError: Boolean
)

object ToastUiMapper {
    fun map(message: ToastMessage): UiToastModel {
        return when (message) {
            is ToastMessage.Error -> {
                UiToastModel(
                    id = message.id,
                    text = ErrorMessageMapper.map(message.error),
                    icon = Lucide.CircleAlert,
                    iconColor = null, // Will use theme destructive color
                    isError = true
                )
            }

            is ToastMessage.Info -> {
                UiToastModel(
                    id = message.id,
                    text = UiText.VerbatimString(message.message),
                    icon = Lucide.Info,
                    iconColor = null, // Will use theme primary
                    isError = false
                )
            }

            is ToastMessage.Success -> {
                UiToastModel(
                    id = message.id,
                    text = UiText.VerbatimString(message.message),
                    icon = Lucide.CircleCheck,
                    iconColor = Color(0xFF10B981),
                    isError = false
                )
            }
        }
    }
}