package com.appotato.shared.barcode.scanner

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
public actual fun BarcodeScannerView(
    modifier: Modifier,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // The analyzer outlives recomposition, so it must not capture a stale lambda.
    val currentOnBarcodeScanned by rememberUpdatedState(onBarcodeScanned)

    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { useCase ->
                        useCase.setAnalyzer(analysisExecutor) { imageProxy ->
                            imageProxy.scanBarcode(scanner::process) { value ->
                                currentOnBarcodeScanned(value)
                            }
                        }
                    }

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            },
            ContextCompat.getMainExecutor(context)
        )

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            analysisExecutor.shutdown()
            scanner.close()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/**
 * ML Kit needs the rotation the frame was captured at, and the [ImageProxy] must stay open until
 * the detector is finished with it — closing it early drops frames and eventually stalls the
 * analyser.
 */
@SuppressLint("UnsafeOptInUsageError")
private inline fun ImageProxy.scanBarcode(
    process: (InputImage) -> com.google.android.gms.tasks.Task<List<Barcode>>,
    crossinline onBarcode: (String) -> Unit
) {
    val mediaImage = image
    if (mediaImage == null) {
        close()
        return
    }

    process(InputImage.fromMediaImage(mediaImage, imageInfo.rotationDegrees))
        .addOnSuccessListener { barcodes ->
            barcodes.firstNotNullOfOrNull { barcode -> barcode.rawValue }?.let { value -> onBarcode(value) }
        }
        .addOnCompleteListener { close() }
}
