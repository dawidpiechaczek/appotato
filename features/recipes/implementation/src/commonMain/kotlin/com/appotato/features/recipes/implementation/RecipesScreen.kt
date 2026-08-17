package com.appotato.features.recipes.implementation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appotato.features.recipes.implementation.generated.resources.Res
import com.appotato.features.recipes.implementation.generated.resources.recipes_empty_body
import com.appotato.features.recipes.implementation.generated.resources.recipes_empty_title
import com.appotato.features.recipes.implementation.generated.resources.recipes_error_body
import com.appotato.features.recipes.implementation.generated.resources.recipes_error_title
import com.appotato.features.recipes.implementation.generated.resources.recipes_generated_notice
import com.appotato.features.recipes.implementation.generated.resources.recipes_loading
import com.appotato.features.recipes.implementation.generated.resources.recipes_no_ideas_body
import com.appotato.features.recipes.implementation.generated.resources.recipes_no_ideas_title
import com.appotato.features.recipes.implementation.generated.resources.recipes_refresh
import com.appotato.features.recipes.implementation.generated.resources.recipes_retry
import com.appotato.features.recipes.implementation.generated.resources.recipes_subtitle_count
import com.appotato.features.recipes.implementation.generated.resources.recipes_title
import com.appotato.shared.ui.components.AppotatoTheme
import com.appotato.shared.ui.components.Banner
import com.appotato.shared.ui.components.BodyText
import com.appotato.shared.ui.components.ElevatedButton
import com.appotato.shared.ui.components.Loader
import com.appotato.shared.ui.components.ScreenHeader
import com.appotato.shared.ui.components.SubheaderText
import com.appotato.shared.ui.components.TextButton
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Same as the pantry list, so the two tabs line up when you switch between them. */
private val HorizontalPadding = 16.dp
private val ListVerticalPadding = 12.dp
private val MessagePadding = 24.dp
private val ItemSpacing = 12.dp
private val MessageSpacing = 8.dp

@Composable
public fun RecipesRoute(modifier: Modifier = Modifier) {
    val viewModel: RecipesViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The locale is a Compose concern; reading it here keeps `androidx.compose.ui` out of the
    // ViewModel, which is what makes the ViewModel testable under plain `runTest`.
    val languageTag = Locale.current.language

    LaunchedEffect(languageTag) {
        viewModel.onIntent(RecipesIntent.Shown(languageTag))
    }

    RecipesScreen(
        state = state,
        onRefresh = { viewModel.onIntent(RecipesIntent.RefreshClicked) },
        modifier = modifier
    )
}

@Composable
internal fun RecipesScreen(
    state: RecipesState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(Res.string.recipes_title),
            // Says how many items drove the suggestions, so a surprising one is explainable.
            // The urgent count, not the basket size: this is why the screen has anything on it,
            // and it is the same number the pantry badges.
            subtitle = if (state.urgentCount == 0) {
                null
            } else {
                pluralStringResource(Res.plurals.recipes_subtitle_count, state.urgentCount, state.urgentCount)
            },
            action = {
                // Hidden while loading rather than disabled: a spinner already says "working", and
                // a second control saying the same thing is noise.
                if (!state.isLoading && state.hasLoaded) {
                    TextButton(onClick = onRefresh) {
                        BodyText(text = stringResource(Res.string.recipes_refresh))
                    }
                }
            }
        )

        when {
            // Only for the very first load. A refresh keeps whatever is on screen, because
            // replacing readable content with a spinner loses the user's place.
            state.isLoading && !state.hasLoaded -> Centred {
                Loader()
                // Generating takes seconds, not milliseconds. A bare spinner for that long reads
                // as a stall; saying what is happening turns the same wait into progress.
                BodyText(
                    text = stringResource(Res.string.recipes_loading),
                    color = AppotatoTheme.colors.muted,
                    textAlign = TextAlign.Center
                )
            }

            state.failure != null && state.recipes.isEmpty() -> Centred {
                Message(
                    title = stringResource(Res.string.recipes_error_title),
                    body = stringResource(Res.string.recipes_error_body)
                )
                ElevatedButton(onClick = onRefresh) {
                    BodyText(text = stringResource(Res.string.recipes_retry))
                }
            }

            state.isEmptyPantry -> Centred {
                Message(
                    title = stringResource(Res.string.recipes_empty_title),
                    body = stringResource(Res.string.recipes_empty_body)
                )
            }

            state.isEmptyResult -> Centred {
                Message(
                    title = stringResource(Res.string.recipes_no_ideas_title),
                    body = stringResource(Res.string.recipes_no_ideas_body)
                )
            }

            else -> RecipeList(state = state)
        }
    }
}

@Composable
private fun RecipeList(state: RecipesState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = HorizontalPadding, vertical = ListVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(ItemSpacing)
    ) {
        item {
            // States that these were generated rather than looked up. Worth saying once, at the
            // top: a suggestion that reads oddly is easier to forgive when its origin is obvious.
            Banner(
                text = stringResource(Res.string.recipes_generated_notice),
                color = AppotatoTheme.colors.primary,
                leading = "✨",
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(items = state.recipes, key = { recipe -> recipe.title }) { recipe ->
            RecipeCard(recipe = recipe)
        }
    }
}

@Composable
private fun Centred(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(MessagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MessageSpacing, Alignment.CenterVertically)
    ) {
        content()
    }
}

@Composable
private fun Message(title: String, body: String) {
    SubheaderText(text = title, textAlign = TextAlign.Center)
    BodyText(text = body, color = AppotatoTheme.colors.muted, textAlign = TextAlign.Center)
}
