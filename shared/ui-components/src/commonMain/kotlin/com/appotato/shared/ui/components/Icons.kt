package com.appotato.shared.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.appotato.shared.ui.components.generated.resources.Res
import com.appotato.shared.ui.components.generated.resources.ic_add
import com.appotato.shared.ui.components.generated.resources.ic_delete
import com.appotato.shared.ui.components.generated.resources.ic_pantry
import com.appotato.shared.ui.components.generated.resources.ic_recipes
import com.appotato.shared.ui.components.generated.resources.ic_scan
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * [contentDescription] is deliberately not optional. Every icon in this app is either the only
 * label a control has, or decoration next to a real one — and only the call site knows which, so
 * it has to say (pass null for decoration).
 */
@Composable
fun Icon(
    icon: AppotatoIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    // Inherits from the surrounding slot — a navigation item tints its own icon on selection, and
    // hard-coding the brand colour here would fight it.
    tint: Color = LocalContentColor.current,
) = Icon(
    modifier = modifier,
    painter = painterResource(icon.drawable()),
    contentDescription = contentDescription,
    tint = tint,
)

/**
 * XML vector drawables, not SVG: compose-resources rejects SVG on Android at runtime, and the
 * failure only shows up on first composition.
 */
private fun AppotatoIcon.drawable(): DrawableResource = when (this) {
    AppotatoIcon.Pantry -> Res.drawable.ic_pantry
    AppotatoIcon.Scan -> Res.drawable.ic_scan
    AppotatoIcon.Recipes -> Res.drawable.ic_recipes
    AppotatoIcon.Add -> Res.drawable.ic_add
    AppotatoIcon.Delete -> Res.drawable.ic_delete
}
