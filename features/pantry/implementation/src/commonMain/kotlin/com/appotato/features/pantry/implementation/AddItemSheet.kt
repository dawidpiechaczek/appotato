package com.appotato.features.pantry.implementation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appotato.features.pantry.implementation.generated.resources.Res
import com.appotato.features.pantry.implementation.generated.resources.pantry_add_action
import com.appotato.features.pantry.implementation.generated.resources.pantry_add_days_label
import com.appotato.features.pantry.implementation.generated.resources.pantry_add_name_label
import com.appotato.features.pantry.implementation.generated.resources.pantry_add_quantity_label
import com.appotato.features.pantry.implementation.generated.resources.pantry_add_scanned
import com.appotato.features.pantry.implementation.generated.resources.pantry_add_title
import com.appotato.shared.ui.components.AppotatoTheme
import com.appotato.shared.ui.components.BodyText
import com.appotato.shared.ui.components.Chip
import com.appotato.shared.ui.components.CommentText
import com.appotato.shared.ui.components.ElevatedButton
import com.appotato.shared.ui.components.OutlinedTextField
import com.appotato.shared.ui.components.SubheaderText
import org.jetbrains.compose.resources.stringResource

private val SheetPadding = 16.dp
private val ItemSpacing = 12.dp
private val DaysFieldWidth = 96.dp

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
