package com.appotato.shared.billing.api

/**
 * What the user currently owns.
 *
 * [entitlements] sits on the interface so that gating never has to `when` over the subtypes — a
 * feature asks "is Pro in there", and only screens that explain the state (paywall, settings) look
 * at which case it is.
 */
public sealed interface SubscriptionStatus {

    public val entitlements: Set<Entitlement>

    /** Free tier: never subscribed, or a store that has nothing to say about this user. */
    public data object None : SubscriptionStatus {
        override val entitlements: Set<Entitlement> = emptySet()
    }

    /**
     * The subscription grants access right now.
     *
     * [willRenew] is false once the user cancels — access continues until [expiresAtEpochMillis],
     * which is the window a win-back prompt has to work with. [isInGracePeriod] means the renewal
     * charge failed but the store is still retrying; access is kept and the user should be asked
     * to fix their payment method, not shown a paywall.
     */
    public data class Active(
        public val planId: String,
        override val entitlements: Set<Entitlement>,
        public val expiresAtEpochMillis: Long,
        public val willRenew: Boolean,
        public val isTrial: Boolean = false,
        public val isInGracePeriod: Boolean = false
    ) : SubscriptionStatus

    /** Access has lapsed. Distinct from [None] so win-back copy can tell the two apart. */
    public data class Expired(
        public val planId: String,
        public val expiredAtEpochMillis: Long
    ) : SubscriptionStatus {
        override val entitlements: Set<Entitlement> = emptySet()
    }
}
