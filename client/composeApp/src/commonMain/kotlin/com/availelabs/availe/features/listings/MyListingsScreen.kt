package com.availelabs.availe.features.listings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.composables.uikit.components.PrimaryButton
import com.composables.uikit.components.Text

@Composable
fun MyListingsScreen(component: IMyListingsHostComponent) {
    val childStack by component.childStack.subscribeAsState()

    Children(
        stack = childStack,
    ) {
        when (val child = it.instance) {
            is MyListingsChild.List -> UserListingsScreen(child.component)
            is MyListingsChild.Create -> CreateListingScreen(child.component)
        }
    }
}

@Composable
fun UserListingsScreen(component: IUserListingsComponent) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PrimaryButton(onClick = { component.onCreateClicked() }) {
            Text("Create New Listing")
        }
    }
}

@Composable
fun CreateListingScreen(component: ICreateListingComponent) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PrimaryButton(onClick = { component.onFinished() }) {
            Text("Finish Creation (Placeholder)")
        }
    }
}