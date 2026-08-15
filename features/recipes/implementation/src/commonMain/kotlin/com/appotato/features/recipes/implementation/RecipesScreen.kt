package com.appotato.features.recipes.implementation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appotato.features.recipes.implementation.generated.resources.Res
import com.appotato.features.recipes.implementation.generated.resources.recipes_empty_body
import com.appotato.features.recipes.implementation.generated.resources.recipes_empty_title
import com.appotato.features.recipes.implementation.generated.resources.recipes_title
import com.appotato.shared.ui.components.AppotatoTheme
import com.appotato.shared.ui.components.BodyText
import com.appotato.shared.ui.components.ScreenHeader
import com.appotato.shared.ui.components.SubheaderText
import org.jetbrains.compose.resources.stringResource

private val ContentPadding = 24.dp
private val ItemSpacing = 8.dp

/**
 * A placeholder, and honest about it.
 *
 * The tab exists because the navigation does; suggesting recipes needs a source of recipes, which
 * is a separate decision (own database, or an API and what to do about its licensing). There is
 * deliberately no ViewModel and no fake data here — a screen that pretends to work is worse than
 * one that says it does not yet.
 */
@Composable
public fun RecipesRoute(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(Res.string.recipes_title))
        Column(
            modifier = Modifier.fillMaxSize().padding(ContentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ItemSpacing, Alignment.CenterVertically),
        ) {
            SubheaderText(
                text = stringResource(Res.string.recipes_empty_title),
                textAlign = TextAlign.Center,
            )
            BodyText(
                text = stringResource(Res.string.recipes_empty_body),
                color = AppotatoTheme.colors.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
