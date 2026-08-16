package com.appotato.features.pantry.implementation

internal sealed interface PantryIntent {
    data class NameChanged(val name: String) : PantryIntent
    data class DaysChanged(val days: String) : PantryIntent
    data class QuantityChanged(val quantity: String) : PantryIntent
    data class CaloriesChanged(val calories: String) : PantryIntent
    data class CategorySelected(val category: ProductCategory) : PantryIntent
    /** Null clears the filter — "Wszystkie". */
    data class CategoryFilterSelected(val category: ProductCategory?) : PantryIntent
    data object AddSheetOpened : PantryIntent
    data object AddSheetDismissed : PantryIntent
    data object AddClicked : PantryIntent
    data class DeleteClicked(val id: String) : PantryIntent
    data object UpgradeClicked : PantryIntent
}
