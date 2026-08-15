package com.appotato.shared.barcode.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
public actual fun BarcodeScannerView(
    modifier: Modifier,
    onBarcodeScanned: (String) -> Unit
) {
    val currentOnBarcodeScanned by rememberUpdatedState(onBarcodeScanned)
    val delegate = remember { BarcodeDelegate { value -> currentOnBarcodeScanned(value) } }
    val session = remember { AVCaptureSession() }

    DisposableEffect(session) {
        session.configureForBarcodes(delegate)
        session.startRunningOnBackgroundQueue()
        onDispose { session.stopRunning() }
    }

    UIKitView(
        factory = { CameraPreviewView(session) },
        modifier = modifier,
    )
}

/**
 * A plain [UIView] whose only job is to keep the preview layer the size of itself. Doing it in
 * `layoutSubviews` rather than through the interop callback means the layer follows rotation and
 * safe-area changes the same way any other UIKit view would.
 */
@OptIn(ExperimentalForeignApi::class)
private class CameraPreviewView(session: AVCaptureSession) : UIView(frame = CGRectZero.readValue()) {

    private val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    init {
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        // Without this the layer animates to every new size, which looks like a lag on rotation.
        CATransaction.begin()
        CATransaction.setValue(true, kCATransactionDisableActions)
        previewLayer.setFrame(bounds)
        CATransaction.commit()
    }
}

@OptIn(ExperimentalForeignApi::class)
private class BarcodeDelegate(
    private val onBarcode: (String) -> Unit
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        didOutputMetadataObjects
            .filterIsInstance<AVMetadataMachineReadableCodeObject>()
            .firstNotNullOfOrNull { code -> code.stringValue }
            ?.let(onBarcode)
    }
}

/** The formats a grocery product actually carries, plus QR for the odd own-brand label. */
private val ScannedFormats = listOf(
    AVMetadataObjectTypeEAN13Code,
    AVMetadataObjectTypeEAN8Code,
    AVMetadataObjectTypeUPCECode,
    AVMetadataObjectTypeCode128Code,
    AVMetadataObjectTypeQRCode
)

@OptIn(ExperimentalForeignApi::class)
private fun AVCaptureSession.configureForBarcodes(delegate: AVCaptureMetadataOutputObjectsDelegateProtocol) {
    beginConfiguration()
    sessionPreset = AVCaptureSessionPresetHigh

    val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
    val input = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, null) }
    if (input != null && canAddInput(input)) addInput(input)

    val output = AVCaptureMetadataOutput()
    if (canAddOutput(output)) {
        addOutput(output)
        output.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
        // Has to happen after addOutput: before that the session reports no available types.
        output.metadataObjectTypes = ScannedFormats.filter { it in output.availableMetadataObjectTypes }
    }

    commitConfiguration()
}

/** `startRunning` blocks until the camera is up — several hundred milliseconds of frozen UI. */
@OptIn(ExperimentalForeignApi::class)
private fun AVCaptureSession.startRunningOnBackgroundQueue() {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0uL)) {
        startRunning()
    }
}
