package com.appotato.shared.billing.api

/**
 * Why a [Billing] call failed.
 *
 * The split exists mostly for [Cancelled]: backing out of the store sheet is the single most
 * common outcome of tapping "Subscribe", and showing an error for it is the fastest way to make a
 * paywall look broken. Everything else is grouped by what the UI can offer — retry, fix payment,
 * or restore.
 */
public sealed class BillingError(
    message: String,
    cause: Throwable? = null
) : Throwable(message, cause) {

    /** The user dismissed the store sheet. Not a failure — say nothing. */
    public class Cancelled : BillingError("Purchase cancelled by the user")

    /** The store could not be reached, or billing is unavailable on this device. Offer a retry. */
    public class Unavailable(cause: Throwable? = null) : BillingError("The store is unavailable", cause)

    /** The store refused: declined payment, parental controls, unsupported storefront. */
    public class NotAllowed(cause: Throwable? = null) : BillingError("The store refused the purchase", cause)

    /** This entitlement is already owned — the user needs a restore, not a second purchase. */
    public class AlreadyOwned : BillingError("The subscription is already active")

    /** Anything the implementation could not map onto the cases above. */
    public class Unknown(cause: Throwable? = null) : BillingError("Unknown billing failure", cause)
}
