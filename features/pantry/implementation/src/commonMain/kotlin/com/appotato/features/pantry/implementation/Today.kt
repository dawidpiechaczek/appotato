package com.appotato.features.pantry.implementation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Today's date, injected rather than read inline so a test can sit on either side of an expiry
 * boundary without waiting for midnight.
 */
internal fun interface Today {
    operator fun invoke(): LocalDate
}

internal class SystemToday : Today {

    // kotlinx-datetime 0.7 hands Clock and Instant back to the standard library, where they are
    // still experimental. The opt-in stays here rather than spreading through the feature.
    @OptIn(ExperimentalTime::class)
    override fun invoke(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
