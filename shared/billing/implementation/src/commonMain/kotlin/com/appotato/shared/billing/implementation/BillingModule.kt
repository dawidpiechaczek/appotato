package com.appotato.shared.billing.implementation

import com.appotato.shared.billing.api.Billing
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Binds [Billing].
 *
 * A single binding for both platforms, unlike telemetry and remote config: a billing SDK that
 * covers StoreKit and Play Billing from `commonMain` is the whole reason to use one, so there is
 * nothing for Swift to implement here.
 *
 * This file is the one place a vendor may be named. Swapping the store layer means replacing
 * [NoOpBilling] below and nothing else.
 */
public fun billingModule(): Module = module {
    single<Billing> { NoOpBilling() }
}
