package com.appotato.shared.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual fun coroutineMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
internal actual fun coroutineDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
internal actual fun coroutineIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
internal actual fun coroutineUnconfinedDispatcher(): CoroutineDispatcher = Dispatchers.Unconfined
