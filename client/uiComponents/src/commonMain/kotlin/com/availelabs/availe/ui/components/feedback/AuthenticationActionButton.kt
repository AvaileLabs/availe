package com.availelabs.availe.ui.components.feedback

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import availe.client.designsystem.generated.resources.Res
import availe.client.designsystem.generated.resources.authentication_log_in
import com.composables.icons.lucide.LogIn
import com.composables.icons.lucide.Lucide
import com.composables.uikit.components.Icon
import com.composables.uikit.components.IntermediateProgressIndicator
import com.composables.uikit.components.PrimaryButton
import com.composables.uikit.components.Text
import org.jetbrains.compose.resources.stringResource

@Composable
fun AuthenticationActionButton(
    isLoading: Boolean,
    onLoginClicked: () -> Unit,
    onCancelLoginClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryButton(
        onClick = if (isLoading) onCancelLoginClicked else onLoginClicked,
        enabled = true,
        modifier = modifier.alpha(if (isLoading) 0.5f else 1.0f)
    ) {
        if (isLoading) {
            IntermediateProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            Icon(
                imageVector = Lucide.LogIn,
                contentDescription = null
            )
            Text(stringResource(Res.string.authentication_log_in))
        }
    }
}