package com.availelabs.availe.ui.decompose

import com.arkivanov.decompose.ComponentContext

interface ILoadingComponent

class LoadingComponent(
    componentContext: ComponentContext
) : ILoadingComponent, ComponentContext by componentContext