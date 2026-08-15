package com.appotato.shared.barcode.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Camera permission, as a screen needs to reason about it.
 *
 * [isPermanentlyDenied] is separate from "not granted" because the two need different copy: one
 * asks again, the other has to send the user to system settings, since asking again does nothing.
 */
@Stable
public interface CameraPermissionState {

    public val isGranted: Boolean

    /** Asking again would be a no-op — the user has to change this in system settings. */
    public val isPermanentlyDenied: Boolean

    /** Shows the system prompt. Safe to call when already granted; it does nothing. */
    public fun request()
}

@Composable
public expect fun rememberCameraPermissionState(): CameraPermissionState
