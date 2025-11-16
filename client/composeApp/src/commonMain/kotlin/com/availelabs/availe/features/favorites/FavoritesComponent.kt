package com.availelabs.availe.features.favorites

import com.arkivanov.decompose.ComponentContext

interface IFavoritesComponent

class FavoritesComponent(
    componentContext: ComponentContext,
) : IFavoritesComponent, ComponentContext by componentContext