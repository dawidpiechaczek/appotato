package com.appotato.shared.dispatchers

import kotlinx.coroutines.CoroutineDispatcher

public data class CoroutineDispatchers(
    val main: CoroutineDispatcher = coroutineMainDispatcher(),
    val default: CoroutineDispatcher = coroutineDefaultDispatcher(),
    val io: CoroutineDispatcher = coroutineIoDispatcher(),
    val unconfined: CoroutineDispatcher = coroutineUnconfinedDispatcher(),
)

internal expect fun coroutineMainDispatcher(): CoroutineDispatcher
internal expect fun coroutineDefaultDispatcher(): CoroutineDispatcher
internal expect fun coroutineIoDispatcher(): CoroutineDispatcher
internal expect fun coroutineUnconfinedDispatcher(): CoroutineDispatcher
