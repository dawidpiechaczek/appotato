package com.appotato.shared.billing.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The only two things a gated feature should ever need from [Billing]. Everything richer than a
 * boolean belongs to the paywall and to settings.
 */
public fun Billing.observeAccessTo(entitlement: Entitlement): Flow<Boolean> =
    status.map { entitlement in it.entitlements }.distinctUntilChanged()

/**
 * Synchronous read of the same value, for the places that cannot suspend — a Compose gate on first
 * composition, or a decision made while building a screen.
 */
public fun Billing.hasAccessTo(entitlement: Entitlement): Boolean =
    entitlement in status.value.entitlements
