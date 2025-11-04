package com.availelabs.availe.ui.components

import com.arkivanov.decompose.ComponentContext

interface ILoadingComponent

class LoadingComponent(
    componentContext: ComponentContext
) : ILoadingComponent, ComponentContext by componentContext