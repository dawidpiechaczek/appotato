package com.appotato.features.pantry.implementation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appotato.features.pantry.implementation.generated.resources.Res
import com.appotato.features.pantry.implementation.generated.resources.scanner_hint
import com.appotato.features.pantry.implementation.generated.resources.scanner_permission_denied
import com.appotato.features.pantry.implementation.generated.resources.scanner_permission_grant
import com.appotato.features.pantry.implementation.generated.resources.scanner_permission_rationale
import com.appotato.features.pantry.implementation.generated.resources.scanner_title
import com.appotato.shared.barcode.scanner.BarcodeScannerView
import com.appotato.shared.barcode.scanner.CameraPermissionState
import com.appotato.shared.barcode.scanner.rememberCameraPermissionState
import com.appotato.shared.ui.components.AppotatoTheme
import com.appotato.shared.ui.components.BodyText
import com.appotato.shared.ui.components.CommentText
import com.appotato.shared.ui.components.ElevatedButton
import com.appotato.shared.ui.components.ScreenHeader
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val ScannerPadding = 24.dp
private val ContentSpacing = 16.dp

/**
 * The scanner tab. [onScanned] tells the host to switch to the pantry, where the add sheet opens
 * with the code already attached — the hand-off itself goes through `PendingScan`, not through a
 * parameter, because the two tabs have separate ViewModels.
 */
@Composable
public fun ScannerRoute(
    onScanned: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ScannerViewModel = koinViewModel()
    val permission = rememberCameraPermissionState()

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(Res.string.scanner_title))

        if (permission.isGranted) {
            CameraPane(
                onBarcodeScanned = { barcode ->
                    // The camera fires repeatedly at the same label; only the first read navigates.
                    if (viewModel.onBarcodeScanned(barcode)) onScanned()
                },
            )
        } else {
            PermissionPane(permission = permission)
        }
    }
}

@Composable
private fun CameraPane(onBarcodeScanned: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        BarcodeScannerView(
            modifier = Modifier.fillMaxSize(),
            onBarcodeScanned = onBarcodeScanned,
        )
        CommentText(
            modifier = Modifier.fillMaxWidth().padding(ScannerPadding),
            text = stringResource(Res.string.scanner_hint),
            color = AppotatoTheme.colors.onOverlay,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PermissionPane(permission: CameraPermissionState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(ScannerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ContentSpacing, Alignment.CenterVertically),
    ) {
        BodyText(
            text = if (permission.isPermanentlyDenied) {
                stringResource(Res.string.scanner_permission_denied)
            } else {
                stringResource(Res.string.scanner_permission_rationale)
            },
            textAlign = TextAlign.Center,
        )
        // Asking again after a permanent denial does nothing, so the button goes away rather than
        // becoming a control that silently fails.
        if (!permission.isPermanentlyDenied) {
            ElevatedButton(onClick = permission::request) {
                BodyText(text = stringResource(Res.string.scanner_permission_grant))
            }
        }
    }
}
