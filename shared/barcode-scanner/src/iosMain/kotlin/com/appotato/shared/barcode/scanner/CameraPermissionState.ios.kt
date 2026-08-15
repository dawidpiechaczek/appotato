package com.appotato.shared.barcode.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
public actual fun rememberCameraPermissionState(): CameraPermissionState {
    var status by remember { mutableStateOf(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) }

    return remember(status) {
        object : CameraPermissionState {
            override val isGranted: Boolean = status == AVAuthorizationStatusAuthorized

            // iOS only ever shows the prompt once. After that, Denied and Restricted both mean the
            // answer can only change in Settings.
            override val isPermanentlyDenied: Boolean =
                status == AVAuthorizationStatusDenied || status == AVAuthorizationStatusRestricted

            override fun request() {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { _ ->
                    // The callback lands on an arbitrary queue; Compose state has to be touched
                    // from the main one.
                    dispatch_async(dispatch_get_main_queue()) {
                        status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
                    }
                }
            }
        }
    }
}
