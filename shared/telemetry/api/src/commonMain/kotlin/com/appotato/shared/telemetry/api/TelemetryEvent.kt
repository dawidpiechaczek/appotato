package com.appotato.shared.telemetry.api

/**
 * A single analytics event.
 *
 * Parameters are strings because that is the intersection every backend accepts — Firebase also
 * takes Long and Double, PostHog takes arbitrary JSON, and modelling the union here would leak
 * one vendor's type system into the contract.
 *
 * Features define their own events rather than extending a shared enum here: a sealed hierarchy
 * in this module would force every feature to edit shared code, which breaks the layering rule.
 */
public data class TelemetryEvent(
    public val name: String,
    public val parameters: Map<String, String> = emptyMap()
)
