package com.appotato.features.pantry.implementation

import androidx.lifecycle.ViewModel

internal class ScannerViewModel(private val pendingScan: PendingScan) : ViewModel() {

    /**
     * The camera keeps reporting the same label for as long as it is pointed at it, so only the
     * first read counts until the pantry has taken it.
     */
    fun onBarcodeScanned(barcode: String): Boolean {
        if (pendingScan.barcode.value != null) return false
        pendingScan.submit(barcode)
        return true
    }
}
