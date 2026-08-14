package com.appotato

import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.appotato.features.pantry.implementation.PantryRoute
import com.appotato.features.paywall.implementation.PaywallRoute
import com.appotato.shared.ui.components.AppotatoTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The whole app is one screen plus a paywall over it. This is deliberately not a navigation graph
 * yet — there is one destination, and a library added now would only be guessed at.
 */
@Composable
@Preview
fun App() {
    AppotatoTheme {
        var showPaywall by remember { mutableStateOf(false) }

        if (showPaywall) {
            PaywallRoute(
                modifier = Modifier.safeContentPadding(),
                onSubscribed = { showPaywall = false },
                onDismissed = { showPaywall = false },
            )
        } else {
            PantryRoute(
                modifier = Modifier.safeContentPadding(),
                onPaywallRequested = { showPaywall = true },
            )
        }
    }
}
