package com.appotato.features.pantry.implementation

internal sealed interface PantryIntent {
    data class NameChanged(val name: String) : PantryIntent
    data class DaysChanged(val days: String) : PantryIntent
    data object AddClicked : PantryIntent
    data class DeleteClicked(val id: String) : PantryIntent
    data object UpgradeClicked : PantryIntent
}
