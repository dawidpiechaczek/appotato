package com.appotato.features.pantry.implementation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.appotato.features.pantry.implementation.generated.resources.Res
import com.appotato.features.pantry.implementation.generated.resources.pantry_badge_days
import com.appotato.features.pantry.implementation.generated.resources.pantry_badge_expired
import com.appotato.features.pantry.implementation.generated.resources.pantry_badge_today
import com.appotato.features.pantry.implementation.generated.resources.pantry_delete_action
import com.appotato.features.pantry.implementation.generated.resources.pantry_item_meta
import com.appotato.shared.ui.components.AppotatoIcon
import com.appotato.shared.ui.components.AppotatoTheme
import com.appotato.shared.ui.components.Badge
import com.appotato.shared.ui.components.BodyText
import com.appotato.shared.ui.components.Card
import com.appotato.shared.ui.components.CommentText
import com.appotato.shared.ui.components.Icon
import com.appotato.shared.ui.components.TextButton
import org.jetbrains.compose.resources.stringResource

private val CardPadding = 12.dp
private val ItemSpacing = 8.dp
private val CategoryIconSize = 32.dp
private val DeleteIconSize = 20.dp

@Composable
internal fun PantryCard(entry: PantryEntry, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
        ) {
            BodyText(modifier = Modifier.size(CategoryIconSize), text = entry.item.category.icon)
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

/** Quantity is optional, so the separator has to go with it rather than leaving a dangling dot. */
@Composable
private fun PantryEntry.meta(): String {
    val category = stringResource(item.category.label)
    return if (item.quantity.isBlank()) {
        category
    } else {
        stringResource(Res.string.pantry_item_meta, item.quantity, category)
    }
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
