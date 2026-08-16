package com.appotato.features.pantry.implementation

import androidx.lifecycle.ViewModel

internal class ScannerViewModel(private val pendingScan: PendingScan) : ViewModel() {

    /**
     * The camera keeps reporting the same label for as long as it is pointed at it, so only the
     * first read counts until the pantry has taken it.
     *
     * Note how narrow that is: the pantry takes the code almost immediately and this re-opens, so
     * this only stops reads landing before the hand-off. Deduplicating a barcode against one that
     * is already being looked up is `PantryViewModel.observeScans`'s job, not this one's — the
     * scanner does not know what the form holds.
     */
    fun onBarcodeScanned(barcode: String): Boolean {
        if (pendingScan.barcode.value != null) return false
        pendingScan.submit(barcode)
        return true
    }
}
