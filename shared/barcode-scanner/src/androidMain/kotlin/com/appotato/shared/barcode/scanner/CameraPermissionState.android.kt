package com.appotato.shared.barcode.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
public actual fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var granted by remember { mutableStateOf(context.hasCameraPermission()) }
    var askedAndDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        granted = isGranted
        askedAndDenied = !isGranted
    }

    return remember(granted, askedAndDenied, activity) {
        object : CameraPermissionState {
            override val isGranted: Boolean = granted

            // The system stops showing the rationale once the user has chosen "don't ask again",
            // so "denied and no rationale offered" is the only signal Android gives for it.
            override val isPermanentlyDenied: Boolean = askedAndDenied &&
                activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false

            override fun request() {
                if (!granted) launcher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
