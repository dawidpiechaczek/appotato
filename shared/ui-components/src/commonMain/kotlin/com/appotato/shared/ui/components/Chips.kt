package com.appotato.shared.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val ChipPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
private val ChipShape = RoundedCornerShape(percent = 50)

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
    color = if (isSelected) LocalCustomColors.current.primary else LocalCustomColors.current.secondary,
) {
    CommentText(
        modifier = Modifier.padding(ChipPadding),
        text = text,
        color = if (isSelected) LocalCustomColors.current.white else LocalCustomColors.current.primary,
    )
}
