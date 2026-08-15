package com.appotato.features.pantry.implementation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The hand-off between the scanner tab and the pantry tab.
 *
 * They are separate destinations with separate ViewModels, so the barcode cannot be passed as an
 * argument. A single shared holder keeps it inside this module — the alternative, routing it
 * through the host, would put a pantry concern in `composeApp`.
 */
internal class PendingScan {

    private val _barcode = MutableStateFlow<String?>(null)
    val barcode: StateFlow<String?> = _barcode.asStateFlow()

    fun submit(barcode: String) {
        _barcode.value = barcode
    }

    /** Called once the pantry has taken the code, so returning to the tab does not re-apply it. */
    fun consume() {
        _barcode.value = null
    }
}
