package com.appotato.shared.barcode.scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A live camera preview that reports the first readable barcode it sees.
 *
 * Only ever called once [CameraPermissionState.isGranted] is true — neither platform's camera stack
 * fails gracefully when it is not, and the caller has to render the rationale anyway.
 *
 * [onBarcodeScanned] fires on the main thread and can fire repeatedly for the same code while the
 * camera stays pointed at it. Debouncing is the caller's job, because only the caller knows whether
 * a second read means "still the same product" or "the user scanned two things".
 */
@Composable
public expect fun BarcodeScannerView(
    modifier: Modifier = Modifier,
    onBarcodeScanned: (String) -> Unit
)
