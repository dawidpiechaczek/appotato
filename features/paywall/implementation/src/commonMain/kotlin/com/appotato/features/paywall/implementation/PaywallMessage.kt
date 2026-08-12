package com.appotato.features.paywall.implementation

/**
 * What went wrong, as a case rather than a string — the copy belongs to the screen, and the
 * ViewModel stays testable without asserting on wording.
 *
 * There is deliberately no case for a cancelled purchase: the user already knows they backed out.
 */
internal enum class PaywallMessage {
    /** The store did not return the plans. Retryable. */
    PlansUnavailable,

    /** The store refused the purchase, or failed for a reason other than the user cancelling. */
    PurchaseFailed,

    /** Restore succeeded but the store account owns nothing. */
    NothingToRestore,

    /** Restore itself failed. Retryable. */
    RestoreFailed
}
