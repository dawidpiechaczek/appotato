package com.appotato.features.recipes.implementation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appotato.features.recipes.implementation.generated.resources.Res
import com.appotato.features.recipes.implementation.generated.resources.recipes_match
import com.appotato.features.recipes.implementation.generated.resources.recipes_minutes
import com.appotato.features.recipes.implementation.generated.resources.recipes_missing
import com.appotato.features.recipes.implementation.generated.resources.recipes_steps_show
import com.appotato.features.recipes.implementation.generated.resources.recipes_uses
import com.appotato.shared.recipe.source.api.Recipe
import com.appotato.shared.ui.components.AppotatoTheme
import com.appotato.shared.ui.components.Badge
import com.appotato.shared.ui.components.BodyText
import com.appotato.shared.ui.components.Card
import com.appotato.shared.ui.components.CommentText
import com.appotato.shared.ui.components.SubheaderText
import com.appotato.shared.ui.components.Tag
import org.jetbrains.compose.resources.stringResource

/** Matches the pantry card: the two sit in the same list-shaped world and should breathe alike. */
private val CardPadding = 12.dp
private val SectionSpacing = 12.dp
private val TightSpacing = 6.dp
private val TitleGap = 12.dp
private val StepNumberWidth = 20.dp
private const val TAGS_PER_ROW = 2
private const val FULL_MATCH = 100

/**
 * How much of this recipe the user already owns, as a percentage.
 *
 * Deliberately *not* "how much of the pantry it uses": the question this screen answers is "what
 * can I cook right now", and a recipe needing two more trips to the shop is a worse answer than one
 * needing none, however much of the fridge it would clear.
 *
 * Computed here rather than asked of the backend — it is arithmetic over two lists already in hand,
 * and a number the model invented would be one nobody could check.
 */
internal fun Recipe.matchPercent(): Int {
    val total = usesIngredients.size + missingIngredients.size
    return if (total == 0) 0 else (usesIngredients.size * FULL_MATCH) / total
}

@Composable
internal fun RecipeCard(recipe: Recipe, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        // The shared Card is a bare surface with no padding of its own, so every consumer sets its
        // own — without this the text runs into the card's edge.
        Column(
            modifier = Modifier.fillMaxWidth().padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing)
        ) {
            TitleRow(recipe)

            // Absent rather than guessed when the source would not commit to a number.
            recipe.minutes?.let { minutes ->
                CommentText(
                    text = "⏱ " + stringResource(Res.string.recipes_minutes, minutes),
                    color = AppotatoTheme.colors.muted
                )
            }

            BodyText(text = recipe.summary, color = AppotatoTheme.colors.muted)

            Ingredients(recipe)

            HorizontalDivider(color = AppotatoTheme.colors.outline)

            Steps(recipe)
        }
    }
}

@Composable
private fun TitleRow(recipe: Recipe) {
    val match = recipe.matchPercent()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TitleGap),
        verticalAlignment = Alignment.Top
    ) {
        SubheaderText(text = recipe.title, modifier = Modifier.weight(1f))
        Badge(
            text = stringResource(Res.string.recipes_match, match),
            // Green only when there is genuinely nothing to buy; used any more loosely it would
            // stop meaning "cook this one".
            color = if (match == FULL_MATCH) {
                AppotatoTheme.colors.success
            } else {
                AppotatoTheme.colors.muted
            }
        )
    }
}

@Composable
private fun Ingredients(recipe: Recipe) {
    if (recipe.usesIngredients.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(TightSpacing)) {
            CommentText(text = stringResource(Res.string.recipes_uses))
            // Each of these is a pantry row, named exactly as the pantry names it — the backend
            // echoes the display name back for precisely this reason.
            TagRows(recipe.usesIngredients)
        }
    }

    if (recipe.missingIngredients.isNotEmpty()) {
        CommentText(
            text = stringResource(
                Res.string.recipes_missing,
                recipe.missingIngredients.joinToString(", ")
            ),
            color = AppotatoTheme.colors.caution
        )
    }
}

/**
 * Collapsed by default: the steps are the longest part of a card, and at this point the user is
 * choosing which recipe to read, not cooking one.
 */
@Composable
private fun Steps(recipe: Recipe) {
    var isOpen by remember(recipe.title) { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().clickable { isOpen = !isOpen },
        horizontalArrangement = Arrangement.spacedBy(TightSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BodyText(text = if (isOpen) "▾" else "▸", color = AppotatoTheme.colors.primary)
        BodyText(text = stringResource(Res.string.recipes_steps_show), color = AppotatoTheme.colors.primary)
    }

    AnimatedVisibility(visible = isOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(TightSpacing)) {
            recipe.steps.forEachIndexed { index, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(TightSpacing)) {
                    CommentText(
                        text = "${index + 1}.",
                        color = AppotatoTheme.colors.muted,
                        modifier = Modifier.width(StepNumberWidth)
                    )
                    BodyText(text = step)
                }
            }
        }
    }
}

/**
 * Wrapped by hand rather than with a flow layout: the wrapping one is still experimental in Compose
 * Multiplatform, and a fixed two per row reads fine at every width this app runs at.
 */
@Composable
private fun TagRows(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(TightSpacing)) {
        items.chunked(TAGS_PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TightSpacing)) {
                row.forEach { item -> Tag(text = "✓ $item") }
            }
        }
    }
}
