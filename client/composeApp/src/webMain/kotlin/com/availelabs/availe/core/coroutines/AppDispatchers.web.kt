package com.availelabs.availe.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val IoDispatcher: CoroutineDispatcher = Dispatchers.Default