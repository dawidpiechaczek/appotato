package com.appotato.shared.network

import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Binds the one [HttpClient] the app has.
 *
 * A `single` rather than a `factory`: an engine owns a connection pool and a thread pool, and one
 * per caller would open a new set of sockets for every lookup. It is never closed — its lifetime is
 * the process, and there is nothing later to close it from.
 */
public fun networkModule(): Module = module {
    single { appotatoHttpClient() }
}
