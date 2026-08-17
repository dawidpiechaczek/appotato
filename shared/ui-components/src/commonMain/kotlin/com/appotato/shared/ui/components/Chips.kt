package com.appotato.shared.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ChipPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
private val ChipShape = RoundedCornerShape(percent = 50)

/**
 * A chip that states something rather than offering a choice — no ripple, no click target, nothing
 * for a screen reader to announce as actionable.
 *
 * Separate from [Chip] because giving that one an empty `onClick` would look identical and behave
 * like a lie: it would take focus, respond to taps and do nothing.
 */
@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalCustomColors.current.onPrimaryContainer,
    background: Color = LocalCustomColors.current.primaryContainer
) = Surface(
    modifier = modifier,
    shape = ChipShape,
    color = background
) {
    CommentText(
        modifier = Modifier.padding(ChipPadding),
        text = text,
        color = color
    )
}

/**
 * A filter chip. Selection is a fill rather than a checkmark — with several chips in a row a tick
 * on each is noise, and the filled one reads as "this is what you are looking at".
 */
@Composable
fun Chip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = Surface(
    modifier = modifier,
    onClick = onClick,
    shape = ChipShape,
    color = if (isSelected) LocalCustomColors.current.primary else LocalCustomColors.current.primaryContainer,
) {
    CommentText(
        modifier = Modifier.padding(ChipPadding),
        text = text,
        color = if (isSelected) {
            LocalCustomColors.current.onPrimary
        } else {
            LocalCustomColors.current.onPrimaryContainer
        },
    )
}
