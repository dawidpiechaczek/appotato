package com.appotato.features.pantry.implementation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.appotato.features.pantry.implementation.generated.resources.Res
import com.appotato.features.pantry.implementation.generated.resources.pantry_badge_days
import com.appotato.features.pantry.implementation.generated.resources.pantry_badge_expired
import com.appotato.features.pantry.implementation.generated.resources.pantry_badge_today
import com.appotato.features.pantry.implementation.generated.resources.pantry_delete_action
import com.appotato.features.pantry.implementation.generated.resources.pantry_item_calories
import com.appotato.features.pantry.implementation.generated.resources.pantry_item_meta
import com.appotato.shared.ui.components.AppotatoIcon
import com.appotato.shared.ui.components.AppotatoTheme
import com.appotato.shared.ui.components.Badge
import com.appotato.shared.ui.components.BodyText
import com.appotato.shared.ui.components.Card
import com.appotato.shared.ui.components.CommentText
import com.appotato.shared.ui.components.EmojiIcon
import com.appotato.shared.ui.components.Icon
import com.appotato.shared.ui.components.TextButton
import com.appotato.shared.ui.components.UrlImage
import org.jetbrains.compose.resources.stringResource

private val CardPadding = 12.dp
private val ItemSpacing = 8.dp
// Tall enough to be the row's height driver: the two text lines next to it come to a little
// less, so the icon plus the card padding is what sets how tall an item is.
private val CategoryIconSize = 40.dp
private val DeleteIconSize = 20.dp
// Rounded rather than a circle: packaging is rectangular, and a circle crops the corners of a label.
private val ThumbnailShape = RoundedCornerShape(8.dp)

@Composable
internal fun PantryCard(entry: PantryEntry, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
        ) {
            ItemThumbnail(item = entry.item)
            Column(modifier = Modifier.weight(weight = 1f)) {
                BodyText(text = entry.item.name)
                CommentText(text = entry.meta(), color = AppotatoTheme.colors.muted)
            }
            Badge(text = entry.badgeLabel(), color = entry.status.color())
            TextButton(onClick = onDelete) {
                Icon(
                    modifier = Modifier.size(DeleteIconSize),
                    icon = AppotatoIcon.Delete,
                    contentDescription = stringResource(Res.string.pantry_delete_action),
                    tint = AppotatoTheme.colors.muted,
                )
            }
        }
    }
}

/**
 * The product's own photo when the scan found one, and the category emoji otherwise.
 *
 * The emoji is also what [UrlImage] shows while the photo loads and if it fails, so a row is never
 * a blank square — offline, the list looks exactly as it did before photos existed.
 */
@Composable
private fun ItemThumbnail(item: PantryItem) {
    val emoji = @Composable { EmojiIcon(emoji = item.category.icon, size = CategoryIconSize) }

    if (item.imageUrl == null) {
        emoji()
    } else {
        UrlImage(
            modifier = Modifier.size(CategoryIconSize).clip(ThumbnailShape),
            url = item.imageUrl,
            // Decorative: the name is right beside it, and reading the URL out loud helps nobody.
            contentDescription = null,
            fallback = emoji,
        )
    }
}

/**
 * Quantity and calories are both optional, so the parts are collected first and only then joined —
 * a missing one has to take its separator with it rather than leaving a dangling dot.
 *
 * Joined by folding the two-part format over the list, in a loop rather than with `reduce`, because
 * `stringResource` is a Composable and cannot be called from an ordinary lambda.
 */
@Composable
private fun PantryEntry.meta(): String {
    val parts = listOfNotNull(
        item.quantity.takeIf { it.isNotBlank() },
        stringResource(item.category.label),
        item.caloriesPer100g?.let { calories -> stringResource(Res.string.pantry_item_calories, calories) }
    )

    var text = parts.first()
    for (index in 1..parts.lastIndex) {
        text = stringResource(Res.string.pantry_item_meta, text, parts[index])
    }
    return text
}

@Composable
private fun PantryEntry.badgeLabel(): String = when {
    daysUntilExpiry < 0 -> stringResource(Res.string.pantry_badge_expired, -daysUntilExpiry)
    daysUntilExpiry == 0 -> stringResource(Res.string.pantry_badge_today)
    else -> stringResource(Res.string.pantry_badge_days, daysUntilExpiry)
}

@Composable
private fun ExpiryStatus.color(): Color = when (this) {
    ExpiryStatus.Expired -> AppotatoTheme.colors.danger
    ExpiryStatus.ExpiringSoon -> AppotatoTheme.colors.caution
    ExpiryStatus.Fresh -> AppotatoTheme.colors.success
}
