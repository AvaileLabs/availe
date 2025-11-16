package com.availelabs.availe.features.messages

import com.arkivanov.decompose.ComponentContext

interface IMessagesComponent

class MessagesComponent(
    componentContext: ComponentContext,
) : IMessagesComponent, ComponentContext by componentContext