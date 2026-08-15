package com.appotato.shared.ui.components

/**
 * The app's icon set, as a closed list.
 *
 * An enum rather than exposing the drawables: the assets stay internal to the design system, so a
 * feature cannot start reaching for arbitrary files and the set stays small enough to keep
 * visually consistent.
 */
enum class AppotatoIcon {
    Pantry,
    Scan,
    Recipes,
    Add,
    Delete
}
