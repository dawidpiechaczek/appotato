package com.appotato.shared.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TextField(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    value: String,
    onValueChange: (String) -> Unit,
) = TextField(
    modifier = modifier,
    colors = TextFieldDefaults.colors(
        focusedTextColor = LocalCustomColors.current.primary,
        focusedLabelColor = LocalCustomColors.current.primary,
    ),
    label = label,
    value = value,
    onValueChange = onValueChange,
    enabled = enabled,
)

@Composable
fun OutlinedTextField(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    value: String,
    onValueChange: (String) -> Unit,
) = OutlinedTextField(
    modifier = modifier,
    colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = LocalCustomColors.current.primary,
        focusedLabelColor = LocalCustomColors.current.primary,
    ),
    label = label,
    value = value,
    onValueChange = onValueChange,
    enabled = enabled,
)