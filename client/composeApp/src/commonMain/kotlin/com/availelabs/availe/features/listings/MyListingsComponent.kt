package com.availelabs.availe.features.listings

import com.arkivanov.decompose.ComponentContext

interface IMyListingsComponent

class MyListingsComponent(
    componentContext: ComponentContext,
) : IMyListingsComponent, ComponentContext by componentContext