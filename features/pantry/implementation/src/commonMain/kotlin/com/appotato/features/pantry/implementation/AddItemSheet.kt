package com.appotato.features.pantry.implementation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.appotato.features.pantry.implementation.generated.resources.Res
import com.appotato.features.pantry.implementation.generated.resources.pantry_add_action
import com.appotato.features.pantry.implementation.generated.resources.pantry_add_calories_label
import com.appotato.features.pantry.implementation.generated.resources.pantry_add_days_label
import com.appotato.features.pantry.implementation.generated.resources.pantry_add_name_label
import com.appotato.features.pantry.implementation.generated.resources.pantry_add_quantity_label
import com.appotato.features.pantry.implementation.generated.resources.pantry_add_scanned
import com.appotato.features.pantry.implementation.generated.resources.pantry_add_title
import com.appotato.features.pantry.implementation.generated.resources.pantry_lookup_failed
import com.appotato.features.pantry.implementation.generated.resources.pantry_lookup_found
import com.appotato.features.pantry.implementation.generated.resources.pantry_lookup_in_progress
import com.appotato.features.pantry.implementation.generated.resources.pantry_lookup_not_found
import com.appotato.shared.ui.components.AppotatoTheme
import com.appotato.shared.ui.components.BodyText
import com.appotato.shared.ui.components.Chip
import com.appotato.shared.ui.components.CommentText
import com.appotato.shared.ui.components.ElevatedButton
import com.appotato.shared.ui.components.Loader
import com.appotato.shared.ui.components.OutlinedTextField
import com.appotato.shared.ui.components.SubheaderText
import com.appotato.shared.ui.components.UrlImage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val SheetPadding = 16.dp
private val ItemSpacing = 12.dp
private val DaysFieldWidth = 96.dp
// Sized to the line of text beside it — the default indicator is a third of the sheet's width.
private val LookupLoaderSize = 16.dp
// Bigger than the list thumbnail: this one is here to be looked at, not to label a row.
private val PreviewImageSize = 48.dp
private val PreviewImageShape = RoundedCornerShape(8.dp)

/**
 * Adding is a sheet rather than a form above the list: on a phone the inline version cost about a
 * third of the screen and pushed the items it was meant to complement below the fold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddItemSheet(
    state: PantryState,
    onIntent: (PantryIntent) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = AppotatoTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(SheetPadding),
            verticalArrangement = Arrangement.spacedBy(ItemSpacing),
        ) {
            SubheaderText(text = stringResource(Res.string.pantry_add_title))

            state.newItemBarcode?.let { barcode ->
                CommentText(
                    text = stringResource(Res.string.pantry_add_scanned, barcode),
                    color = AppotatoTheme.colors.muted,
                )
            }

            LookupNotice(status = state.lookup, imageUrl = state.newItemImageUrl)

            ItemFields(state = state, onIntent = onIntent)

            CategoryPicker(selected = state.newItemCategory, onSelected = { category ->
                onIntent(PantryIntent.CategorySelected(category))
            })

            ElevatedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canAdd,
                onClick = { onIntent(PantryIntent.AddClicked) },
            ) {
                BodyText(text = stringResource(Res.string.pantry_add_action))
            }
        }
    }
}

@Composable
private fun ItemFields(state: PantryState, onIntent: (PantryIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ItemSpacing)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            label = { CommentText(text = stringResource(Res.string.pantry_add_name_label)) },
            value = state.newItemName,
            onValueChange = { name -> onIntent(PantryIntent.NameChanged(name)) },
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(weight = 1f),
                label = { CommentText(text = stringResource(Res.string.pantry_add_quantity_label)) },
                value = state.newItemQuantity,
                onValueChange = { quantity -> onIntent(PantryIntent.QuantityChanged(quantity)) },
            )
            OutlinedTextField(
                modifier = Modifier.width(DaysFieldWidth),
                label = { CommentText(text = stringResource(Res.string.pantry_add_days_label)) },
                value = state.newItemDays,
                onValueChange = { days -> onIntent(PantryIntent.DaysChanged(days)) },
            )
        }

        // Its own row: the label carries the unit, and "kcal / 100 g" does not fit beside the two
        // fields above it on a phone.
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            label = { CommentText(text = stringResource(Res.string.pantry_add_calories_label)) },
            value = state.newItemCalories,
            onValueChange = { calories -> onIntent(PantryIntent.CaloriesChanged(calories)) },
        )
    }
}

/**
 * One line under the scanned code saying how the lookup went, with the product's photo beside it
 * once there is one — the fastest way for the user to see whether the right thing was found.
 *
 * Nothing here ever disables the form: the fields are usable throughout, and the answer only saves
 * typing.
 */
@Composable
private fun LookupNotice(status: LookupStatus, imageUrl: String?) {
    if (status == LookupStatus.Idle) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            status == LookupStatus.InProgress -> Loader(modifier = Modifier.size(LookupLoaderSize))
            imageUrl != null -> UrlImage(
                modifier = Modifier.size(PreviewImageSize).clip(PreviewImageShape),
                url = imageUrl,
                // The line of text next to it already says what this is.
                contentDescription = null,
            )
        }
        CommentText(
            text = stringResource(status.message()),
            color = if (status == LookupStatus.Failed) AppotatoTheme.colors.caution else AppotatoTheme.colors.muted,
        )
    }
}

private fun LookupStatus.message(): StringResource = when (this) {
    // Idle never reaches here — the notice is not shown at all.
    LookupStatus.Idle, LookupStatus.InProgress -> Res.string.pantry_lookup_in_progress
    LookupStatus.Found -> Res.string.pantry_lookup_found
    LookupStatus.NotFound -> Res.string.pantry_lookup_not_found
    LookupStatus.Failed -> Res.string.pantry_lookup_failed
}

@Composable
private fun CategoryPicker(selected: ProductCategory, onSelected: (ProductCategory) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
    ) {
        ProductCategory.entries.forEach { category ->
            Chip(
                text = "${category.icon} ${stringResource(category.label)}",
                isSelected = category == selected,
                onClick = { onSelected(category) },
            )
        }
    }
}
