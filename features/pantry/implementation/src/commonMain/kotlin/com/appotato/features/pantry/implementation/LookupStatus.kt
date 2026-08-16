package com.appotato.features.pantry.implementation

/**
 * How the barcode the user just scanned is getting on.
 *
 * [NotFound] and [Failed] are separate because they ask different things of the user: an uncatalogued
 * product means "type the name yourself", a failed request means "this may work in a moment". Both
 * still leave a usable form — the scan is a convenience, and none of this can block adding an item.
 */
internal enum class LookupStatus {
    /** Nothing scanned, or the sheet was opened by hand. */
    Idle,
    InProgress,
    Found,

    /** The barcode is not in the database. Common for local and own-brand products. */
    NotFound,

    /** The lookup could not be made at all — no connection, or the source was unreachable. */
    Failed
}
