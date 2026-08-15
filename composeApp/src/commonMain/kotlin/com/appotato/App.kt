package com.appotato

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.appotato.features.pantry.implementation.PantryRoute
import com.appotato.features.pantry.implementation.ScannerRoute
import com.appotato.features.paywall.implementation.PaywallRoute
import com.appotato.features.recipes.implementation.RecipesRoute
import com.appotato.shared.ui.components.AppotatoTheme
import com.appotato.shared.ui.components.BottomBar
import com.appotato.shared.ui.components.BottomBarItem
import com.appotato.shared.ui.components.Screen
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    AppotatoTheme {
        // rememberSaveable: the selected tab has to survive rotation and process death, which is
        // the one thing a plain remember would lose.
        var tab by rememberSaveable { mutableStateOf(AppTab.Pantry) }
        var showPaywall by rememberSaveable { mutableStateOf(false) }

        if (showPaywall) {
            // A takeover, without the bottom bar: leaving is the paywall's own decision to offer.
            PaywallRoute(
                modifier = Modifier.fillMaxSize(),
                onSubscribed = { showPaywall = false },
                onDismissed = { showPaywall = false },
            )
        } else {
            Screen(
                bottomBar = {
                    BottomBar {
                        AppTab.entries.forEach { entry ->
                            BottomBarItem(
                                icon = entry.icon,
                                label = stringResource(entry.label),
                                isSelected = entry == tab,
                                onClick = { tab = entry },
                            )
                        }
                    }
                },
            ) { insets ->
                TabContent(
                    tab = tab,
                    insets = insets,
                    onPaywallRequested = { showPaywall = true },
                    onScanned = { tab = AppTab.Pantry },
                )
            }
        }
    }
}

@Composable
private fun TabContent(
    tab: AppTab,
    insets: PaddingValues,
    onPaywallRequested: () -> Unit,
    onScanned: () -> Unit
) {
    val modifier = Modifier.fillMaxSize().padding(insets)
    when (tab) {
        AppTab.Pantry -> PantryRoute(modifier = modifier, onPaywallRequested = onPaywallRequested)
        AppTab.Scan -> ScannerRoute(modifier = modifier, onScanned = onScanned)
        AppTab.Recipes -> RecipesRoute(modifier = modifier)
    }
}
