package com.appotato.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ElevatedButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) = ElevatedButton(
    modifier = modifier,
    colors = ButtonDefaults.elevatedButtonColors(
        contentColor = LocalCustomColors.current.primary,
        containerColor = LocalCustomColors.current.secondary,
    ),
    enabled = enabled,
    onClick = onClick,
    content = content,
)

@Composable
fun OutlinedButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) = OutlinedButton(
    modifier = modifier,
    colors = ButtonDefaults.outlinedButtonColors(
        contentColor = LocalCustomColors.current.primary,
        containerColor = LocalCustomColors.current.white,
    ),
    border = BorderStroke(width = 2.dp, color = LocalCustomColors.current.primary),
    enabled = enabled,
    onClick = onClick,
    content = content,
)

@Composable
fun TextButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) = TextButton(
    modifier = modifier,
    colors = ButtonDefaults.textButtonColors(
        contentColor = LocalCustomColors.current.primary,
        containerColor = LocalCustomColors.current.transparent,
    ),
    enabled = enabled,
    onClick = onClick,
    content = content,
)
