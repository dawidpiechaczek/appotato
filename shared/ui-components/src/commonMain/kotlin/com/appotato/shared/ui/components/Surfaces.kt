package com.appotato.shared.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BadgePadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)

/** Container for one row of content. Elevation comes from the theme, not from the call site. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) = Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = LocalCustomColors.current.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = LocalCustomElevation.current.small),
    content = { content() },
)

/**
 * A short status label. The background is [color] at low opacity rather than a second theme entry,
 * so any semantic colour can be badged without adding a matching tint for it.
 */
@Composable
fun Badge(
    modifier: Modifier = Modifier,
    text: String,
    color: Color
) = Surface(
    modifier = modifier,
    color = color.copy(alpha = BADGE_BACKGROUND_ALPHA),
    shape = MaterialTheme.shapes.small,
) {
    CommentText(
        modifier = Modifier.padding(BadgePadding),
        text = text,
        color = color,
    )
}

private const val BADGE_BACKGROUND_ALPHA = 0.15f
