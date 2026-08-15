package com.appotato.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BannerShape = RoundedCornerShape(12.dp)
private val BannerPadding = 12.dp
private val BannerSpacing = 8.dp
private const val BANNER_BACKGROUND_ALPHA = 0.12f

/**
 * A standing notice about the screen's contents — not a transient message, so it has no dismiss.
 * The colour carries the urgency and the border keeps it legible against a tinted background.
 */
@Composable
fun Banner(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    leading: String = "⚠️"
) = Surface(
    modifier = modifier,
    shape = BannerShape,
    color = color.copy(alpha = BANNER_BACKGROUND_ALPHA),
    border = BorderStroke(width = 1.dp, color = color),
) {
    Row(
        modifier = Modifier.padding(BannerPadding),
        horizontalArrangement = Arrangement.spacedBy(BannerSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodyText(text = leading)
        BodyText(text = text, color = color)
    }
}
