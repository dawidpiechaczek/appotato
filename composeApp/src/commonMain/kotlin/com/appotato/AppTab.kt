package com.appotato

import appotato.composeapp.generated.resources.Res
import appotato.composeapp.generated.resources.tab_pantry
import appotato.composeapp.generated.resources.tab_recipes
import appotato.composeapp.generated.resources.tab_scan
import com.appotato.shared.ui.components.AppotatoIcon
import org.jetbrains.compose.resources.StringResource

/**
 * The app's top-level destinations.
 *
 * Three flat tabs with no back stack between them, so this is an enum and a `remember` rather than
 * a navigation library. That changes the moment a tab needs to push a second screen, a deep link
 * has to resolve, or the paywall stops being a full-screen takeover — at which point
 * `org.jetbrains.androidx.navigation:navigation-compose` is the thing to reach for.
 */
internal enum class AppTab(val icon: AppotatoIcon, val label: StringResource) {
    Pantry(AppotatoIcon.Pantry, Res.string.tab_pantry),
    Scan(AppotatoIcon.Scan, Res.string.tab_scan),
    Recipes(AppotatoIcon.Recipes, Res.string.tab_recipes)
}
