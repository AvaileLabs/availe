package com.availelabs.availe.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual val IoDispatcher: CoroutineDispatcher = Dispatchers.IO