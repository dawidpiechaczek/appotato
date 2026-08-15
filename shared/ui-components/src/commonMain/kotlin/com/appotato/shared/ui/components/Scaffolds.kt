package com.appotato.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val HeaderPadding = 16.dp
private val ActionSize = 48.dp
private val ActionIconSize = 24.dp

/**
 * Page frame: an optional header, an optional bottom bar, and the content between them.
 *
 * Both slots are nullable rather than defaulting to an empty lambda, so "this screen has no bottom
 * bar" is something the caller states instead of an empty block that still reserves a slot.
 */
@Composable
fun Screen(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) = Scaffold(
    modifier = modifier,
    containerColor = LocalCustomColors.current.background,
    topBar = { header?.invoke() },
    bottomBar = { bottomBar?.invoke() },
    content = content,
)

/**
 * Title, an optional line of context under it, and one optional action on the right. Not a
 * `TopAppBar`: the subtitle is part of the design and material3's title slot is a single line.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) = Row(
    modifier = modifier.fillMaxWidth().padding(HeaderPadding),
    verticalAlignment = Alignment.CenterVertically,
) {
    Column(modifier = Modifier.weight(weight = 1f)) {
        HeaderText(text = title)
        if (subtitle != null) {
            CommentText(text = subtitle, color = LocalCustomColors.current.muted)
        }
    }
    action?.invoke()
}

/** The round button in the header. One per screen — it is the screen's primary action. */
@Composable
fun CircularAction(
    icon: AppotatoIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = Surface(
    modifier = modifier.size(ActionSize),
    onClick = onClick,
    shape = CircleShape,
    color = LocalCustomColors.current.primary,
) {
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.size(ActionIconSize),
            icon = icon,
            contentDescription = contentDescription,
            tint = LocalCustomColors.current.onPrimary,
        )
    }
}

@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) = NavigationBar(
    modifier = modifier,
    containerColor = LocalCustomColors.current.surface,
    content = content,
)

@Composable
fun RowScope.BottomBarItem(
    icon: AppotatoIcon,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) = NavigationBarItem(
    selected = isSelected,
    onClick = onClick,
    icon = { Icon(icon = icon, contentDescription = null) },
    label = { CommentText(text = label) },
    colors = NavigationBarItemDefaults.colors(
        selectedIconColor = LocalCustomColors.current.onPrimaryContainer,
        selectedTextColor = LocalCustomColors.current.primary,
        unselectedIconColor = LocalCustomColors.current.muted,
        unselectedTextColor = LocalCustomColors.current.muted,
        indicatorColor = LocalCustomColors.current.primaryContainer,
    ),
)
