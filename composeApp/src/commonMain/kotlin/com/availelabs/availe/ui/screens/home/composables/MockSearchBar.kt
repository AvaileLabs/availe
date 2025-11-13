package com.availelabs.availe.ui.screens.home.composables

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.uikit.components.Icon
import com.composables.uikit.components.Text
import com.composables.uikit.components.TextField

@Composable
internal fun MockSearchBar(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }

    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier,
        placeholder = { Text("Search...") },
        leading = {
            Icon(
                imageVector = Lucide.Search,
                contentDescription = "Search"
            )
        },
        singleLine = true
    )
}